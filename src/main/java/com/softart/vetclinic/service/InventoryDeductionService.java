package com.softart.vetclinic.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.entity.ServiceInventoryItem;
import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.InventoryTransactionRepository;
import com.softart.vetclinic.repository.ServiceInventoryItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryDeductionService {

    private final ServiceInventoryItemRepository serviceInventoryItemRepo;
    private final InventoryItemRepository itemRepo;
    private final InventoryTransactionRepository txRepo;
    private final InventoryBatchRepository batchRepo;
    private final InventoryStockApplier stockApplier;

    // ===================== DEDUKCIJA =====================

    /**
     * BOM dedukcija pri kreiranju usluge na intervenciji.
     * service_inventory_item nosi product_id + quantity_per_use → razreši inventory_item
     * po lokaciji (ZA SADA: jedini/glavni; multi-lokacija → Faza 2E) → FEFO + preliv na DEFAULT.
     */
    @Transactional
    public void deductForTreatment(UUID clinicId, UUID serviceId, UUID treatmentId, UUID performedBy) {
        List<ServiceInventoryItem> mappings = serviceInventoryItemRepo
                .findByClinicIdAndServiceIdAndDeletedFalse(clinicId, serviceId);
        if (mappings.isEmpty()) return;

        for (ServiceInventoryItem mapping : mappings) {
            InventoryItem item = resolveItemForProduct(clinicId, mapping.getProductId());
            if (item == null) {
                log.warn("BOM: nema inventory_item za product {} (klinika {}) — preskačem dedukciju",
                        mapping.getProductId(), clinicId);
                continue;
            }
            deductFromItem(clinicId, item, mapping.getQuantityPerUse(), "TREATMENT", treatmentId, performedBy);
        }
        log.info("Auto-dedukcija inventara za treatment {} — {} mappings", treatmentId, mappings.size());
    }

    /**
     * Dedukcija pri direktnoj prodaji artikla (Quick Sale POS / ručna fakturna stavka).
     * Prima već razrešen per-lokacijski inventory_item id.
     */
    @Transactional
    public void deductForInvoiceItem(UUID clinicId, UUID inventoryItemId,
            BigDecimal quantity, UUID invoiceItemId, UUID performedBy) {
        InventoryItem item = itemRepo.findByIdAndClinicIdAndDeletedFalse(inventoryItemId, clinicId).orElse(null);
        if (item == null) return;
        deductFromItem(clinicId, item, quantity, "INVOICE_ITEM", invoiceItemId, performedBy);
        log.info("Auto-dedukcija inventara za invoice_item {} — količina {}", invoiceItemId, quantity);
    }

    /**
     * Jezgro: FEFO troši realne (ne-DEFAULT) lotove (expiry ASC, NULLS LAST),
     * a ostatak/preliv pada na DEFAULT lot (sme u minus — preživljava do reconciliation-a).
     * Ne grana se po track_batches: ne-batch artikal nema realne lotove → sve ide na DEFAULT.
     * Pozvati unutar @Transactional; lotovi se hvataju PESSIMISTIC_WRITE lock-om.
     */
    private void deductFromItem(UUID clinicId, InventoryItem item, BigDecimal needed,
            String referenceType, UUID referenceId, UUID performedBy) {
        BigDecimal remaining = needed;

        List<InventoryBatch> batches = batchRepo.findActiveNonDefaultByItemFifoForUpdate(clinicId, item.getId());
        for (InventoryBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal take = remaining.min(batch.getQuantityOnHand());
            stockApplier.applyToBatch(batch, InventoryTransactionType.OUT, take);
            batchRepo.save(batch);
            saveOutTx(clinicId, item.getId(), batch.getId(), take, referenceType, referenceId, performedBy,
                    "Auto-dedukcija (FEFO) — lot " + batch.getBatchNumber());
            remaining = remaining.subtract(take);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            InventoryBatch def = batchRepo.findDefaultByItemForUpdate(clinicId, item.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "DEFAULT lot ne postoji za artikal " + item.getId()));
            stockApplier.applyToBatch(def, InventoryTransactionType.OUT, remaining);
            batchRepo.save(def);
            saveOutTx(clinicId, item.getId(), def.getId(), remaining, referenceType, referenceId, performedBy,
                    "Auto-dedukcija — preliv na DEFAULT lot");
            if (def.getQuantityOnHand().compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Negativan lager (DEFAULT lot) za artikal {} — stanje {}",
                        item.getId(), def.getQuantityOnHand());
            }
        }
    }

    private void saveOutTx(UUID clinicId, UUID itemId, UUID batchId, BigDecimal qty,
            String referenceType, UUID referenceId, UUID performedBy, String note) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setClinicId(clinicId);
        tx.setInventoryItemId(itemId);
        tx.setBatchId(batchId);
        tx.setType(InventoryTransactionType.OUT);
        tx.setQuantity(qty);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        tx.setPerformedBy(performedBy);
        tx.setNote(note);
        txRepo.save(tx);
    }

    /**
     * ZA SADA jedan inventory_item po product-u (1:1 posle migracije).
     * Multi-lokacija (izbor reda po lokaciji pružanja usluge) → Faza 2E.
     */
    private InventoryItem resolveItemForProduct(UUID clinicId, UUID productId) {
        List<InventoryItem> items = itemRepo.findByClinicIdAndProductIdAndDeletedFalse(clinicId, productId);
        return items.isEmpty() ? null : items.get(0);
    }

    // ===================== REVERZIJA =====================

    @Transactional
    public void reverseForTreatment(UUID clinicId, UUID treatmentId) {
        reverseByReference(clinicId, "TREATMENT", treatmentId);
    }

    @Transactional
    public void reverseForInvoiceItem(UUID clinicId, UUID invoiceItemId) {
        reverseByReference(clinicId, "INVOICE_ITEM", invoiceItemId);
    }

    /**
     * Poništava sve OUT transakcije po referenci i vraća količine na njihove lotove.
     * Posle splita tx UVEK ima batch_id → reverzija je isključivo batch-level.
     */
    private void reverseByReference(UUID clinicId, String referenceType, UUID referenceId) {
        List<InventoryTransaction> txs = txRepo
                .findByClinicIdAndReferenceTypeAndReferenceIdAndDeletedFalse(clinicId, referenceType, referenceId);
        if (txs.isEmpty()) return;

        for (InventoryTransaction tx : txs) {
            tx.setDeleted(true);
            tx.setDeletedAt(OffsetDateTime.now());
            txRepo.save(tx);

            if (tx.getBatchId() != null) {
                batchRepo.findByIdAndClinicIdAndDeletedFalseForUpdate(tx.getBatchId(), clinicId)
                        .ifPresent(batch -> {
                            stockApplier.reverseOnBatch(batch, tx.getType(), tx.getQuantity());
                            batchRepo.save(batch);
                        });
            }
            // tx bez batch_id ne postoji posle splita (defanzivno: nema šta da se vrati na lot)
        }
        log.info("Reverzija inventara za {} {} — {} transakcija poništeno",
                referenceType, referenceId, txs.size());
    }
}