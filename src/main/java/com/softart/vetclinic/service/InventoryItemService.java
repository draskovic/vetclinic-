package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.repository.InventoryItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryItemService extends AbstractCrudService<InventoryItem, InventoryItemRepository> {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository) {
        super(inventoryItemRepository);
        this.inventoryItemRepository = inventoryItemRepository;
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

    @Transactional(readOnly = true)
    public List<InventoryItem> findByCategory(UUID clinicId, InventoryCategory category) {
        return inventoryItemRepository.findByClinicIdAndCategoryAndActiveTrue(clinicId, category);
    }
}
