package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryTransaction;
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

@Service
public class InventoryTransactionService extends AbstractCrudService<InventoryTransaction, InventoryTransactionRepository> {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryTransactionService(InventoryTransactionRepository inventoryTransactionRepository,
                                       InventoryItemRepository inventoryItemRepository) {
        super(inventoryTransactionRepository);
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
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
    }
    
    @Override
    @Transactional
    public InventoryTransaction create(InventoryTransaction entity, UUID clinicId) {
        InventoryTransaction saved = super.create(entity, clinicId);

        inventoryItemRepository.findByIdAndClinicIdAndDeletedFalse(entity.getInventoryItemId(), clinicId)
                .ifPresent(item -> {
                	switch (entity.getType()) {
                    case IN -> item.setQuantityOnHand(item.getQuantityOnHand().add(entity.getQuantity()));
                    case OUT, EXPIRED -> item.setQuantityOnHand(item.getQuantityOnHand().subtract(entity.getQuantity()));
                    case ADJUSTMENT -> item.setQuantityOnHand(entity.getQuantity());
                }

                    inventoryItemRepository.save(item);
                });

        return saved;
    }

    @Override
    @Transactional
    public InventoryTransaction update(UUID id, UUID clinicId, java.util.function.Consumer<InventoryTransaction> updater) {
        // Dohvati stare vrednosti PRE izmene
        InventoryTransaction existing = findById(id, clinicId);
        UUID oldItemId = existing.getInventoryItemId();
        InventoryTransactionType oldType = existing.getType();
        java.math.BigDecimal oldQty = existing.getQuantity();

        // Primeni update (ovo menja existing objekat)
        InventoryTransaction saved = super.update(id, clinicId, updater);

        // Poništi staru transakciju
        inventoryItemRepository.findByIdAndClinicIdAndDeletedFalse(oldItemId, clinicId)
                .ifPresent(item -> {
                    switch (oldType) {
                        case IN -> item.setQuantityOnHand(item.getQuantityOnHand().subtract(oldQty));
                        case OUT, EXPIRED -> item.setQuantityOnHand(item.getQuantityOnHand().add(oldQty));
                        case ADJUSTMENT -> {}
                    }
                    inventoryItemRepository.save(item);
                });

        // Primeni novu transakciju
        inventoryItemRepository.findByIdAndClinicIdAndDeletedFalse(saved.getInventoryItemId(), clinicId)
                .ifPresent(item -> {
                    switch (saved.getType()) {
                        case IN -> item.setQuantityOnHand(item.getQuantityOnHand().add(saved.getQuantity()));
                        case OUT, EXPIRED -> item.setQuantityOnHand(item.getQuantityOnHand().subtract(saved.getQuantity()));
                        case ADJUSTMENT -> item.setQuantityOnHand(saved.getQuantity());
                    }
                    inventoryItemRepository.save(item);
                });

        return saved;
    }


    @Transactional(readOnly = true)
    public List<InventoryTransaction> findByItem(UUID clinicId, UUID inventoryItemId) {
        return inventoryTransactionRepository.findByClinicIdAndInventoryItemIdAndDeletedFalse(clinicId, inventoryItemId);
    }
}
