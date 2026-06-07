package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.InventoryItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryBatchService extends AbstractCrudService<InventoryBatch, InventoryBatchRepository> {

    private final InventoryBatchRepository batchRepository;
    private final InventoryItemRepository itemRepository;

    public InventoryBatchService(InventoryBatchRepository batchRepository,
                                 InventoryItemRepository itemRepository) {
        super(batchRepository);
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    protected String getEntityName() {
        return "InventoryBatch";
    }

    @Override
    protected Optional<InventoryBatch> findByIdAndClinicId(UUID id, UUID clinicId) {
        return batchRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<InventoryBatch> findAllByClinicId(UUID clinicId, Pageable pageable) {
        throw new UnsupportedOperationException("Use findByItem instead");
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return batchRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<InventoryBatch> findByItem(UUID clinicId, UUID itemId) {
        return batchRepository.findByItem(clinicId, itemId);
    }

    @Transactional(readOnly = true)
    public List<InventoryBatch> findExpiring(UUID clinicId, int daysThreshold) {
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return batchRepository.findExpiringBefore(clinicId, threshold);
    }

    @Transactional(readOnly = true)
    public long countExpiring(UUID clinicId, int daysThreshold) {
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return batchRepository.countExpiringBefore(clinicId, threshold);
    }

    /**
     * Kreira novi lot. Stanje artikla se NE sinhronizuje — računa se na čitanju kao SUM(lotova).
     */
    @Transactional
    public InventoryBatch createBatch(InventoryBatch batch, UUID clinicId) {
        if (!itemRepository.existsByIdAndClinicIdAndDeletedFalse(batch.getInventoryItemId(), clinicId)) {
            throw new BadRequestException("Artikal ne postoji");
        }

        Optional<InventoryBatch> existing = batchRepository
                .findByClinicIdAndInventoryItemIdAndBatchNumberAndDeletedFalse(
                        clinicId, batch.getInventoryItemId(), batch.getBatchNumber());
        if (existing.isPresent()) {
            throw new DuplicateResourceException("InventoryBatch", "batchNumber", batch.getBatchNumber());
        }

        if (batch.getQuantityOnHand() == null || batch.getQuantityOnHand().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Količina mora biti >= 0");
        }
        if (batch.getReceivedAt() == null) {
            batch.setReceivedAt(LocalDate.now());
        }

        batch.setClinicId(clinicId);
        batch.setDeleted(false);
        return batchRepository.save(batch);
    }

    /**
     * Soft-delete lota — dozvoljen samo ako quantityOnHand == 0.
     */
    @Transactional
    public void deleteBatch(UUID id, UUID clinicId) {
        InventoryBatch batch = findById(id, clinicId);
        if (batch.getQuantityOnHand() != null && batch.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Lot ima preostalu količinu — ne može se obrisati");
        }
        softDelete(id, clinicId);
    }

    /**
     * Stanje artikla = SUM(quantityOnHand aktivnih lotova). Minus (na DEFAULT lotu) preživljava.
     */
    @Transactional(readOnly = true)
    public BigDecimal getQuantityByItem(UUID clinicId, UUID itemId) {
        BigDecimal qty = batchRepository.sumQuantityByItem(clinicId, itemId);
        return qty != null ? qty : BigDecimal.ZERO;
    }

    /**
     * Batch-fetch stanja za listu artikala (1 GROUP BY upit). Artikli bez reda → 0.
     */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> getQuantitiesByItemIds(UUID clinicId, Collection<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (InventoryBatchRepository.ItemQuantity row : batchRepository.sumQuantityByItemIds(clinicId, itemIds)) {
            result.put(row.getItemId(), row.getQty() != null ? row.getQty() : BigDecimal.ZERO);
        }
        for (UUID id : itemIds) {
            result.putIfAbsent(id, BigDecimal.ZERO);
        }
        return result;
    }
}