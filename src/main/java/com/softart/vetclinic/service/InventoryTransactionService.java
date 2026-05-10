package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.InventoryTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.softart.vetclinic.enums.InventoryTransactionType;


import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.softart.vetclinic.exception.BadRequestException;

@Service
public class InventoryTransactionService extends AbstractCrudService<InventoryTransaction, InventoryTransactionRepository> {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryBatchService inventoryBatchService;
    private final InventoryStockApplier stockApplier;

    public InventoryTransactionService(InventoryTransactionRepository inventoryTransactionRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryBatchRepository inventoryBatchRepository,
            InventoryBatchService inventoryBatchService,
            InventoryStockApplier stockApplier) {
				super(inventoryTransactionRepository);
				this.inventoryTransactionRepository = inventoryTransactionRepository;
				this.inventoryItemRepository = inventoryItemRepository;
				this.inventoryBatchRepository = inventoryBatchRepository;
				this.inventoryBatchService = inventoryBatchService;
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

    private void validateBatch(InventoryTransaction entity) {
        var item = inventoryItemRepository.findByIdAndClinicIdAndDeletedFalse(
                entity.getInventoryItemId(), entity.getClinicId())
            .orElseThrow();
        validateBatch(entity, item);
    }

    /**
     * Varijanta sa već-učitanim item-om — eliminiše N+1 kad se validacija
     * poziva više puta u istom flow-u (create, update).
     */
    private void validateBatch(InventoryTransaction entity, com.softart.vetclinic.entity.InventoryItem item) {
        if (Boolean.TRUE.equals(item.getTrackBatches())) {
            if (entity.getBatchId() == null) {
                throw new BadRequestException("Za artikle sa lotovima, lot je obavezan");
            }
        }

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
                    "Razlog (reason) je obavezan za transakcije tipa " + type.name()
            );
        }
    }

    
    @Override
    @Transactional
    public InventoryTransaction create(InventoryTransaction entity, UUID clinicId) {
        InventoryTransaction saved = super.create(entity, clinicId);

        if (entity.getBatchId() != null) {
            // Batch-aware tx — primeni delta na lot, pa sinhronizuj item.qty = SUM(batches)
            inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalseForUpdate(entity.getBatchId(), clinicId)
                    .ifPresent(batch -> {
                        stockApplier.applyToBatch(batch, entity.getType(), entity.getQuantity());
                        inventoryBatchRepository.save(batch);
                    });
            inventoryItemRepository
                    .findByIdAndClinicIdAndDeletedFalse(entity.getInventoryItemId(), clinicId)
                    .ifPresent(item -> inventoryBatchService.syncItemQuantity(clinicId, item));
        } else {
            // Item-level tx — direktna primena na quantityOnHand
            inventoryItemRepository
                    .findByIdAndClinicIdAndDeletedFalse(entity.getInventoryItemId(), clinicId)
                    .ifPresent(item -> {
                        stockApplier.applyToItem(item, entity.getType(), entity.getQuantity());
                        inventoryItemRepository.save(item);
                    });
        }

        return saved;
    }

    @Override
    @Transactional
    public InventoryTransaction update(UUID id, UUID clinicId, java.util.function.Consumer<InventoryTransaction> updater) {
        // Snapshot starih vrednosti PRE primene update-a
        InventoryTransaction existing = findById(id, clinicId);
        UUID oldItemId = existing.getInventoryItemId();
        UUID oldBatchId = existing.getBatchId();
        InventoryTransactionType oldType = existing.getType();
        java.math.BigDecimal oldQty = existing.getQuantity();

        // Primeni update (ovo menja existing objekat)
        InventoryTransaction saved = super.update(id, clinicId, updater);
        validateReason(saved);
        validateBatch(saved);

        // 1) Poništi efekat STARE transakcije (na starom batch-u ili starom item-u)
        if (oldBatchId != null) {
            inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalseForUpdate(oldBatchId, clinicId)
                    .ifPresent(batch -> {
                        stockApplier.reverseOnBatch(batch, oldType, oldQty);
                        inventoryBatchRepository.save(batch);
                    });
        } else {
            inventoryItemRepository
                    .findByIdAndClinicIdAndDeletedFalse(oldItemId, clinicId)
                    .ifPresent(item -> {
                        stockApplier.reverseOnItem(item, oldType, oldQty);
                        inventoryItemRepository.save(item);
                    });
        }

        // 2) Primeni efekat NOVE transakcije (na novom batch-u ili novom item-u)
        if (saved.getBatchId() != null) {
            inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalseForUpdate(saved.getBatchId(), clinicId)
                    .ifPresent(batch -> {
                        stockApplier.applyToBatch(batch, saved.getType(), saved.getQuantity());
                        inventoryBatchRepository.save(batch);
                    });
        } else {
            inventoryItemRepository
                    .findByIdAndClinicIdAndDeletedFalse(saved.getInventoryItemId(), clinicId)
                    .ifPresent(item -> {
                        stockApplier.applyToItem(item, saved.getType(), saved.getQuantity());
                        inventoryItemRepository.save(item);
                    });
        }

        // 3) Sinhronizuj item.quantityOnHand iz lotova — za stari item (uvek ako batch flow)
        //    i za novi item ako se promenio
        if (oldBatchId != null) {
            inventoryItemRepository
                    .findByIdAndClinicIdAndDeletedFalse(oldItemId, clinicId)
                    .ifPresent(item -> inventoryBatchService.syncItemQuantity(clinicId, item));
        }
        if (saved.getBatchId() != null && !saved.getInventoryItemId().equals(oldItemId)) {
            inventoryItemRepository
                    .findByIdAndClinicIdAndDeletedFalse(saved.getInventoryItemId(), clinicId)
                    .ifPresent(item -> inventoryBatchService.syncItemQuantity(clinicId, item));
        }

        return saved;
    }

    @Transactional
    public InventoryTransaction reverse(UUID id, UUID clinicId) {
        // Dohvati original sa lock-om (serijalizacija paralelnih storno zahteva)
        InventoryTransaction original = inventoryTransactionRepository
                .findByIdAndClinicIdAndDeletedFalseForUpdate(id, clinicId)
                .orElseThrow(() -> new BadRequestException("Transakcija ne postoji"));

        // Validacije
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

        // Edge case — obrisan lot
        if (original.getBatchId() != null) {
            boolean batchExists = inventoryBatchRepository
                    .findByIdAndClinicIdAndDeletedFalse(original.getBatchId(), clinicId)
                    .isPresent();
            if (!batchExists) {
                throw new BadRequestException(
                        "Storniranje onemogućeno jer je pripadajući lot obrisan.");
            }
        }

        // Kreiraj storno: obrnut tip, ista količina, isti batch/item
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

        // Prolazi kroz standardni create flow — primenjuje se na batch/item
        InventoryTransaction saved = create(storno, clinicId);

        // Obeleži original kao storniran (u istoj transakciji)
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
