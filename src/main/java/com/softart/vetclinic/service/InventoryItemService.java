package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.enums.AdjustmentReason;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.exception.BadRequestException;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.function.Consumer;


import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryItemService extends AbstractCrudService<InventoryItem, InventoryItemRepository> {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionService inventoryTransactionService;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository,
                                @Lazy InventoryTransactionService inventoryTransactionService) {
        super(inventoryItemRepository);
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryTransactionService = inventoryTransactionService;
    }


    @Override
    protected String getEntityName() {
        return "InventoryItem";
    }

    @Override
    protected Optional<InventoryItem> findByIdAndClinicId(UUID id, UUID clinicId) {
        return inventoryItemRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<InventoryItem> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return inventoryItemRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return inventoryItemRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    /**
     * Atomsko kreiranje artikla sa opcionim početnim stanjem.
     * Forsira quantityOnHand = 0 i, ako je initialQuantity > 0 i artikal NE prati lotove,
     * kreira IN transakciju sa reason=OPENING_BALANCE (audit trail).
     *
     * Za trackBatches=true artikle, initialQuantity se ignoriše — zaliha se unosi
     * kroz "Lotovi" tab (svaki lot je zaseban zapis sa batch_id).
     */
    @Transactional
    public InventoryItem createWithInitialQuantity(InventoryItem entity, UUID clinicId, BigDecimal initialQuantity) {
        // 1) Uvek forsiraj quantityOnHand = 0 pri kreiranju — jedini validan put
        //    do zaliha je kroz inventory_transaction (audit trail)
        entity.setQuantityOnHand(BigDecimal.ZERO);

        // 2) Kreiraj artikal kroz standardni flow (RLS, validacije, audit)
        InventoryItem saved = super.create(entity, clinicId);

        // 3) Opcioni OPENING_BALANCE — samo ako ima vrednost i artikal ne prati lotove
        boolean tracksBatches = Boolean.TRUE.equals(saved.getTrackBatches());
        if (initialQuantity != null
                && initialQuantity.compareTo(BigDecimal.ZERO) > 0
                && !tracksBatches) {

            InventoryTransaction openingTx = new InventoryTransaction();
            openingTx.setInventoryItemId(saved.getId());
            openingTx.setType(InventoryTransactionType.IN);
            openingTx.setQuantity(initialQuantity);
            openingTx.setReason(AdjustmentReason.OPENING_BALANCE);
            openingTx.setNote("Otvaranje kartice artikla");

            // InventoryTransactionService.create automatski povećava quantityOnHand
            // kroz postojeći switch (IN → add), pa ne moramo ručno
            inventoryTransactionService.create(openingTx, clinicId);
        }

        return saved;
    }

    /**
     * Override sa dve zaštite na postojećem artiklu:
     *
     * 1) quantityOnHand se NE sme menjati direktno kroz DTO — sve promene zaliha
     *    MORAJU ići kroz inventory_transaction (audit trail). Vrednost iz DTO-a se
     *    ignoriše i vraća na originalnu.
     *
     * 2) trackBatches (način praćenja) se NE sme menjati posle kreiranja — flip bi
     *    pregazio quantityOnHand sa SUM(lotova) i tiho izgubio stanje. Pokušaj → 400.
     */
    @Override
    @Transactional
    public InventoryItem update(UUID id, UUID clinicId, Consumer<InventoryItem> updater) {
        InventoryItem existing = findById(id, clinicId);
        BigDecimal originalQty = existing.getQuantityOnHand();
        Boolean originalTrackBatches = existing.getTrackBatches();

        Consumer<InventoryItem> protectedUpdater = item -> {
            updater.accept(item);

            // R2: track_batches se NE sme menjati na postojećem artiklu.
            // Flip bi pregazio quantity_on_hand sa SUM(lotova) -> tiha nula
            // (vidi "TestFIFO" iz dijagnostike). Novi nacin pracenja = novi artikal.
            if (!originalTrackBatches.equals(item.getTrackBatches())) {
                throw new BadRequestException(
                        "Praćenje po lotovima se ne može menjati na postojećem artiklu. "
                        + "Napravite novi artikal sa željenim načinom praćenja.");
            }

            // Forsirano vraćanje — sve promene zaliha idu kroz inventory_transaction
            item.setQuantityOnHand(originalQty);
        };

        return super.update(id, clinicId, protectedUpdater);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findByCategory(UUID clinicId, InventoryCategory category) {
        return inventoryItemRepository.findByClinicIdAndCategoryAndActiveTrue(clinicId, category);
    }
    
    public Page<InventoryItem> searchAll(UUID clinicId, String search, InventoryCategory category, Pageable pageable) {
        if ((search == null || search.isBlank()) && category == null) {
            return findAllByClinicId(clinicId, pageable);
        }
        return inventoryItemRepository.searchByClinicIdAndCategory(clinicId,
                (search == null || search.isBlank()) ? "" : search,
                category,
                pageable);
    }
    
    @Transactional(readOnly = true)
    public List<InventoryItem> findLowStock(UUID clinicId) {
        return ((InventoryItemRepository) repository).findLowStock(clinicId);
    }

    @Transactional(readOnly = true)
    public long countLowStock(UUID clinicId) {
        return ((InventoryItemRepository) repository).countLowStock(clinicId);
    }


}
