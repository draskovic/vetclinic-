package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.enums.AdjustmentReason;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.ProductRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryItemService extends AbstractCrudService<InventoryItem, InventoryItemRepository> {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionService inventoryTransactionService;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository,
                                InventoryBatchRepository inventoryBatchRepository,
                                ProductRepository productRepository,
                                @Lazy InventoryTransactionService inventoryTransactionService) {
        super(inventoryItemRepository);
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.productRepository = productRepository;
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
     * Atomsko kreiranje per-lokacijskog artikla:
     *  1) snimi inventory_item (RLS, audit),
     *  2) kreiraj sistemski DEFAULT lot (nosi ne-batch stanje + preliv/minus),
     *  3) opcioni OPENING_BALANCE (samo ne-batch artikli) → IN tx na DEFAULT lot.
     *
     * Za track_batches artikle initialQuantity se ignoriše — zaliha ide kroz realne lotove.
     */
    @Transactional
    public InventoryItem createWithInitialQuantity(InventoryItem entity, UUID clinicId, BigDecimal initialQuantity) {
        InventoryItem saved = super.create(entity, clinicId);

        InventoryBatch def = new InventoryBatch();
        def.setClinicId(clinicId);
        def.setInventoryItemId(saved.getId());
        def.setBatchNumber("DEFAULT");
        def.setExpiryDate(null);
        def.setQuantityOnHand(BigDecimal.ZERO);
        def.setReceivedAt(LocalDate.now());
        def.setIsDefault(true);
        def.setDeleted(false);
        inventoryBatchRepository.save(def);

        boolean tracksBatches = productRepository
                .findByIdAndClinicIdAndDeletedFalse(saved.getProductId(), clinicId)
                .map(p -> Boolean.TRUE.equals(p.getTrackBatches()))
                .orElse(false);

        if (initialQuantity != null
                && initialQuantity.compareTo(BigDecimal.ZERO) > 0
                && !tracksBatches) {
            InventoryTransaction openingTx = new InventoryTransaction();
            openingTx.setInventoryItemId(saved.getId());
            openingTx.setBatchId(def.getId());
            openingTx.setType(InventoryTransactionType.IN);
            openingTx.setQuantity(initialQuantity);
            openingTx.setReason(AdjustmentReason.OPENING_BALANCE);
            openingTx.setNote("Otvaranje kartice artikla");
            inventoryTransactionService.create(openingTx, clinicId);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findByCategory(UUID clinicId, InventoryCategory category) {
        return inventoryItemRepository.findByClinicIdAndProductCategoryAndActiveTrue(clinicId, category);
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
        return inventoryItemRepository.findLowStock(clinicId);
    }

    @Transactional(readOnly = true)
    public long countLowStock(UUID clinicId) {
        return inventoryItemRepository.countLowStock(clinicId);
    }
}