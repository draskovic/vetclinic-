package com.softart.vetclinic.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

    /**
     * Auto-dedukcija inventara pri kreiranju usluge na intervenciji.
     * Ako artikal ima trackBatches=true → FIFO dedukcija po lotovima.
     * Inače → stari flow iz 1A (direktno sa quantityOnHand).
     */
    @Transactional
    public void deductForTreatment(UUID clinicId, UUID serviceId, UUID treatmentId, UUID performedBy) {
        List<ServiceInventoryItem> mappings = serviceInventoryItemRepo
                .findByClinicIdAndServiceIdAndDeletedFalse(clinicId, serviceId);
        if (mappings.isEmpty()) return;

        for (ServiceInventoryItem mapping : mappings) {
        	Optional<InventoryItem> itemOpt = itemRepo.findByIdAndClinicIdAndDeletedFalseForUpdate(
        	        mapping.getInventoryItemId(), clinicId);
            if (itemOpt.isEmpty()) continue;

            InventoryItem item = itemOpt.get();
            BigDecimal needed = mapping.getQuantityPerUse();

            if (Boolean.TRUE.equals(item.getTrackBatches())) {
                deductFromBatches(clinicId, item, needed, treatmentId, performedBy);
            } else {
                deductDirectly(clinicId, item, needed, treatmentId, performedBy);
            }
        }

        log.info("Auto-dedukcija inventara za treatment {} — {} mappings", treatmentId, mappings.size());
    }

    /**
     * Stari flow iz Faze 1A — direktna dedukcija sa quantityOnHand.
     */
    private void deductDirectly(UUID clinicId, InventoryItem item, BigDecimal needed,
            UUID treatmentId, UUID performedBy) {
		// Atomska provera — pošto je item dohvaćen sa PESSIMISTIC_WRITE lockom,
		// drugi thread-ovi čekaju do commit-a ove transakcije.
    	if (item.getQuantityOnHand().compareTo(needed) < 0) {
    	    log.warn("Negativan lager za {}: ima {}, traži se {} — dozvoljeno",
    	             item.getName(), item.getQuantityOnHand(), needed);
    	}
		
		InventoryTransaction tx = new InventoryTransaction();
		tx.setClinicId(clinicId);
		tx.setInventoryItemId(item.getId());
		tx.setType(InventoryTransactionType.OUT);
		tx.setQuantity(needed);
		tx.setReferenceType("TREATMENT");
		tx.setReferenceId(treatmentId);
		tx.setPerformedBy(performedBy);
		tx.setNote("Auto-dedukcija za uslugu");
		txRepo.save(tx);
		
		item.setQuantityOnHand(item.getQuantityOnHand().subtract(needed));
		itemRepo.save(item);
	}

    /**
     * FIFO dedukcija po lotovima — najpre se troše lotovi sa najbližim rokom.
     * Kreira po jednu OUT transakciju po svakom lotu iz koga se uzima.
     * Posle svih izmena, sinhroniše item.quantityOnHand kao SUM(lots.quantityOnHand).
     */
    private void deductFromBatches(UUID clinicId, InventoryItem item, BigDecimal needed,
                                   UUID treatmentId, UUID performedBy) {
    	// 20260421: NOVO da bi se sprečilo skidanje istog leka kad 10 korisnika istovremeno skida sa lagera:
    	List<InventoryBatch> batches = batchRepo.findActiveByItemFifoForUpdate(clinicId, item.getId());
    	// 20260421: ovo je kraj promene.
    	
    	BigDecimal remaining = needed;
        for (InventoryBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal takeFromBatch = remaining.min(batch.getQuantityOnHand());
            batch.setQuantityOnHand(batch.getQuantityOnHand().subtract(takeFromBatch));
            batchRepo.save(batch);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setClinicId(clinicId);
            tx.setInventoryItemId(item.getId());
            tx.setBatchId(batch.getId());
            tx.setType(InventoryTransactionType.OUT);
            tx.setQuantity(takeFromBatch);
            tx.setReferenceType("TREATMENT");
            tx.setReferenceId(treatmentId);
            tx.setPerformedBy(performedBy);
            tx.setNote("Auto-dedukcija (FIFO) — lot " + batch.getBatchNumber());
            txRepo.save(tx);

            remaining = remaining.subtract(takeFromBatch);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // Nema dovoljno u lotovima — kreiraj OUT bez batch_id za ostatak (negativna potrošnja)
            InventoryTransaction tx = new InventoryTransaction();
            tx.setClinicId(clinicId);
            tx.setInventoryItemId(item.getId());
            tx.setType(InventoryTransactionType.OUT);
            tx.setQuantity(remaining);
            tx.setReferenceType("TREATMENT");
            tx.setReferenceId(treatmentId);
            tx.setPerformedBy(performedBy);
            tx.setNote("Auto-dedukcija — nedostatak u lotovima");
            txRepo.save(tx);
            log.warn("Nedovoljno zaliha u lotovima za {}, nedostaje {}", item.getName(), remaining);
        }

        // Sinhronizuj ukupnu količinu na artiklu
        BigDecimal newTotal = batchRepo.sumQuantityByItem(clinicId, item.getId());
        item.setQuantityOnHand(newTotal != null ? newTotal : BigDecimal.ZERO);
        itemRepo.save(item);
    }

    /**
     * Reverzija auto-dedukcije pri brisanju usluge sa intervencije.
     * Soft-delete-uje OUT transakcije i vraća količine u lotove (ako batchId postoji)
     * ili direktno na artikal (stari flow).
     */
    @Transactional
    public void reverseForTreatment(UUID clinicId, UUID treatmentId) {
        List<InventoryTransaction> txs = txRepo
                .findByClinicIdAndReferenceTypeAndReferenceIdAndDeletedFalse(clinicId, "TREATMENT", treatmentId);

        if (txs.isEmpty()) return;

        for (InventoryTransaction tx : txs) {
            tx.setDeleted(true);
            tx.setDeletedAt(OffsetDateTime.now());
            txRepo.save(tx);

            if (tx.getBatchId() != null) {
                // Vrati u lot (FIFO transakcija)
                batchRepo.findByIdAndClinicIdAndDeletedFalse(tx.getBatchId(), clinicId)
                        .ifPresent(batch -> {
                            batch.setQuantityOnHand(batch.getQuantityOnHand().add(tx.getQuantity()));
                            batchRepo.save(batch);
                        });
                // Sinhronizuj item.quantityOnHand iz lotova
                itemRepo.findByIdAndClinicIdAndDeletedFalse(tx.getInventoryItemId(), clinicId)
                        .ifPresent(item -> {
                            BigDecimal total = batchRepo.sumQuantityByItem(clinicId, item.getId());
                            item.setQuantityOnHand(total != null ? total : BigDecimal.ZERO);
                            itemRepo.save(item);
                        });
            } else {
                // Stari flow — direktno na artikal
                itemRepo.findByIdAndClinicIdAndDeletedFalse(tx.getInventoryItemId(), clinicId)
                        .ifPresent(item -> {
                            item.setQuantityOnHand(item.getQuantityOnHand().add(tx.getQuantity()));
                            itemRepo.save(item);
                        });
            }
        }

        log.info("Reverzija inventara za treatment {} — {} transakcija poništeno", treatmentId, txs.size());
    }
}
