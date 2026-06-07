package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.InventoryTransactionRepository;
import com.softart.vetclinic.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class InventoryTransactionService extends AbstractCrudService<InventoryTransaction, InventoryTransactionRepository> {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ProductRepository productRepository;
    private final InventoryStockApplier stockApplier;

    public InventoryTransactionService(InventoryTransactionRepository inventoryTransactionRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryBatchRepository inventoryBatchRepository,
            ProductRepository productRepository,
            InventoryStockApplier stockApplier) {
        super(inventoryTransactionRepository);
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.productRepository = productRepository;
        this.stockApplier = stockApplier;
    }

    @Override
    protected String getEntityName() {
        return "InventoryTransaction";
    }

    @Override
    protected Optional<InventoryTransaction> findByIdAndClinicId(UUID id, UUID clinicId) {
        return inventoryTransactionRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<InventoryTransaction> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return inventoryTransactionRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return inventoryTransactionRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(InventoryTransaction entity) {
        requireExists(
                inventoryItemRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getInventoryItemId(), entity.getClinicId()),
                "InventoryItem", "id", entity.getInventoryItemId()
        );
        validateBatch(entity);
        validateReason(entity);
    }

    /**
     * Lot mora pripadati artiklu. (track_batches enforcement je u resolveBatchId — pre create-a.)
     */
    private void validateBatch(InventoryTransaction entity) {
        if (entity.getBatchId() != null) {
            var batch = inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalse(entity.getBatchId(), entity.getClinicId())
                    .orElseThrow(() -> new BadRequestException("Lot ne postoji"));
            if (!batch.getInventoryItemId().equals(entity.getInventoryItemId())) {
                throw new BadRequestException("Lot ne pripada izabranom artiklu");
            }
        }
    }

    private void validateReason(InventoryTransaction entity) {
        InventoryTransactionType type = entity.getType();
        if ((type == InventoryTransactionType.ADJUSTMENT || type == InventoryTransactionType.EXPIRED)
                && entity.getReason() == null) {
            throw new BadRequestException(
                    "Razlog (reason) je obavezan za transakcije tipa " + type.name());
        }
    }

    /**
     * Razrešava batch_id kad nije zadat:
     *  - artikal sa lotovima (product.track_batches) → lot je obavezan (400),
     *  - ne-batch artikal → DEFAULT lot (sav IN/OUT/ADJ ide na njega).
     * Posle splita svaka tx ima batch_id.
     */
    private void resolveBatchId(InventoryTransaction entity, UUID clinicId) {
        if (entity.getBatchId() != null) return;

        InventoryItem item = inventoryItemRepository
                .findByIdAndClinicIdAndDeletedFalse(entity.getInventoryItemId(), clinicId)
                .orElseThrow(() -> new BadRequestException("Artikal ne postoji"));

        boolean tracks = productRepository.findByIdAndClinicIdAndDeletedFalse(item.getProductId(), clinicId)
                .map(p -> Boolean.TRUE.equals(p.getTrackBatches()))
                .orElse(false);
        if (tracks) {
            throw new BadRequestException("Za artikle sa lotovima, lot je obavezan");
        }

        InventoryBatch def = inventoryBatchRepository
                .findDefaultByItemForUpdate(clinicId, entity.getInventoryItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "DEFAULT lot ne postoji za artikal " + entity.getInventoryItemId()));
        entity.setBatchId(def.getId());
    }

    @Override
    @Transactional
    public InventoryTransaction create(InventoryTransaction entity, UUID clinicId) {
        resolveBatchId(entity, clinicId);
        InventoryTransaction saved = super.create(entity, clinicId);

        // batch_id je sad uvek postavljen → primeni deltu na lot (stanje artikla = SUM lotova)
        inventoryBatchRepository
                .findByIdAndClinicIdAndDeletedFalseForUpdate(entity.getBatchId(), clinicId)
                .ifPresent(batch -> {
                    stockApplier.applyToBatch(batch, entity.getType(), entity.getQuantity());
                    inventoryBatchRepository.save(batch);
                });

        return saved;
    }

    @Override
    @Transactional
    public InventoryTransaction update(UUID id, UUID clinicId, Consumer<InventoryTransaction> updater) {
        InventoryTransaction existing = findById(id, clinicId);
        UUID oldBatchId = existing.getBatchId();
        InventoryTransactionType oldType = existing.getType();
        BigDecimal oldQty = existing.getQuantity();

        InventoryTransaction saved = super.update(id, clinicId, updater);
        resolveBatchId(saved, clinicId);
        validateReason(saved);
        validateBatch(saved);

        // 1) poništi efekat STARE tx na starom lotu
        if (oldBatchId != null) {
            inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalseForUpdate(oldBatchId, clinicId)
                    .ifPresent(batch -> {
                        stockApplier.reverseOnBatch(batch, oldType, oldQty);
                        inventoryBatchRepository.save(batch);
                    });
        }
        // 2) primeni efekat NOVE tx na novom lotu
        if (saved.getBatchId() != null) {
            inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalseForUpdate(saved.getBatchId(), clinicId)
                    .ifPresent(batch -> {
                        stockApplier.applyToBatch(batch, saved.getType(), saved.getQuantity());
                        inventoryBatchRepository.save(batch);
                    });
        }

        return saved;
    }

    @Transactional
    public InventoryTransaction reverse(UUID id, UUID clinicId) {
        InventoryTransaction original = inventoryTransactionRepository
                .findByIdAndClinicIdAndDeletedFalseForUpdate(id, clinicId)
                .orElseThrow(() -> new BadRequestException("Transakcija ne postoji"));

        if (original.getType() == InventoryTransactionType.ADJUSTMENT) {
            throw new BadRequestException(
                    "Korekcija se ne može stornirati. Greška se ispravlja novom korekcijom.");
        }
        if (original.getReversalOfTransactionId() != null) {
            throw new BadRequestException("Storno transakcija se ne može stornirati.");
        }
        if (original.isReversed()) {
            throw new BadRequestException("Transakcija je već stornirana.");
        }

        if (original.getBatchId() != null) {
            boolean batchExists = inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalse(original.getBatchId(), clinicId)
                    .isPresent();
            if (!batchExists) {
                throw new BadRequestException(
                        "Storniranje onemogućeno jer je pripadajući lot obrisan.");
            }
        }

        InventoryTransaction storno = new InventoryTransaction();
        storno.setInventoryItemId(original.getInventoryItemId());
        storno.setBatchId(original.getBatchId());
        storno.setQuantity(original.getQuantity());
        storno.setType(switch (original.getType()) {
            case IN -> InventoryTransactionType.OUT;
            case OUT, EXPIRED -> InventoryTransactionType.IN;
            case ADJUSTMENT -> throw new IllegalStateException("unreachable");
        });
        storno.setReversalOfTransactionId(original.getId());
        storno.setNote("Storno transakcije " + original.getType().name()
                + " od " + original.getCreatedAt().toLocalDate());

        InventoryTransaction saved = create(storno, clinicId);

        original.setReversed(true);
        inventoryTransactionRepository.save(original);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> findByItem(UUID clinicId, UUID inventoryItemId) {
        return inventoryTransactionRepository.findByClinicIdAndInventoryItemIdAndDeletedFalseOrderByCreatedAtDesc(clinicId, inventoryItemId);
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransaction> searchAll(UUID clinicId, String search,
                                                 InventoryTransactionType type,
                                                 UUID inventoryItemId, Pageable pageable) {
        if ((search == null || search.isBlank()) && type == null && inventoryItemId == null) {
            return findAll(clinicId, pageable);
        }
        return inventoryTransactionRepository.searchAll(clinicId,
                search == null || search.isBlank() ? "" : search,
                type, inventoryItemId, pageable);
    }
}