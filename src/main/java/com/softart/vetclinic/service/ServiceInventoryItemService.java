package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.ServiceInventoryItem;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.ServiceInventoryItemRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class ServiceInventoryItemService extends AbstractCrudService<ServiceInventoryItem, ServiceInventoryItemRepository> {

    private final ServiceInventoryItemRepository repository;
    private final ServiceRepository serviceRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public ServiceInventoryItemService(ServiceInventoryItemRepository repository,
                                       ServiceRepository serviceRepository,
                                       InventoryItemRepository inventoryItemRepository) {
        super(repository);
        this.repository = repository;
        this.serviceRepository = serviceRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    protected String getEntityName() {
        return "ServiceInventoryItem";
    }

    @Override
    protected Optional<ServiceInventoryItem> findByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<ServiceInventoryItem> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return repository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(ServiceInventoryItem entity) {
        requireExists(
            serviceRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), entity.getClinicId()),
            "Service", "id", entity.getServiceId()
        );
        requireExists(
            inventoryItemRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getInventoryItemId(), entity.getClinicId()),
            "InventoryItem", "id", entity.getInventoryItemId()
        );
    }

    @Transactional(readOnly = true)
    public List<ServiceInventoryItem> findByService(UUID clinicId, UUID serviceId) {
        return repository.findByClinicIdAndServiceIdAndDeletedFalse(clinicId, serviceId);
    }

    @Transactional(readOnly = true)
    public List<ServiceInventoryItem> findByInventoryItem(UUID clinicId, UUID inventoryItemId) {
        return repository.findByClinicIdAndInventoryItemIdAndDeletedFalse(clinicId, inventoryItemId);
    }
}
