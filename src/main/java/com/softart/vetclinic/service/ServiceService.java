package com.softart.vetclinic.service;

import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.enums.ServiceCategory;
import com.softart.vetclinic.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class ServiceService extends AbstractCrudService<com.softart.vetclinic.entity.Service, ServiceRepository> {

    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) {
        super(serviceRepository);
        this.serviceRepository = serviceRepository;
    }

    @Override
    protected String getEntityName() {
        return "Service";
    }

    @Override
    protected Optional<com.softart.vetclinic.entity.Service> findByIdAndClinicId(UUID id, UUID clinicId) {
        return serviceRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<com.softart.vetclinic.entity.Service> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return serviceRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return serviceRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(com.softart.vetclinic.entity.Service entity) {
        if (serviceRepository.existsByClinicIdAndNameAndDeletedFalse(entity.getClinicId(), entity.getName())) {
            throw new DuplicateResourceException("Service", "name", entity.getName());
        }
    }

    @Transactional(readOnly = true)
    public List<com.softart.vetclinic.entity.Service> findByCategory(UUID clinicId, ServiceCategory category) {
        return serviceRepository.findByClinicIdAndCategoryAndActiveTrue(clinicId, category);
    }
}
