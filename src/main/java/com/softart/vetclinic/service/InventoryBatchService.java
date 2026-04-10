package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.entity.InventoryItem;
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
import java.util.List;
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
        // Nije podržano — koristi findByItem
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
     * Kreira novi lot:
     * 1) Validira da artikal postoji i pripada klinici
     * 2) Validira da batchNumber nije duplikat za taj artikal
     * 3) Snima lot
     * 4) Sinhronizuje InventoryItem.quantityOnHand = SUM(lots.quantityOnHand)
     */
    @Transactional
    public InventoryBatch createBatch(InventoryBatch batch, UUID clinicId) {
        InventoryItem item = itemRepository.findByIdAndClinicIdAndDeletedFalse(batch.getInventoryItemId(), clinicId)
                .orElseThrow(() -> new BadRequestException("Artikal ne postoji"));

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
        InventoryBatch saved = batchRepository.save(batch);

        syncItemQuantity(clinicId, item);
        return saved;
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

        InventoryItem item = itemRepository.findByIdAndClinicIdAndDeletedFalse(batch.getInventoryItemId(), clinicId)
                .orElse(null);
        if (item != null) {
            syncItemQuantity(clinicId, item);
        }
    }

    /**
     * Rekalkuliše ukupnu količinu na artiklu kao SUM(lots.quantityOnHand).
     * Poziva se iz createBatch, deleteBatch, FIFO dedukcije i reverse-a.
     */
    @Transactional
    public void syncItemQuantity(UUID clinicId, InventoryItem item) {
        BigDecimal total = batchRepository.sumQuantityByItem(clinicId, item.getId());
        item.setQuantityOnHand(total != null ? total : BigDecimal.ZERO);
        itemRepository.save(item);
    }
}
