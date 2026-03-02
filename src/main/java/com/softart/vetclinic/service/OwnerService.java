package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.repository.OwnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OwnerService extends AbstractCrudService<Owner, OwnerRepository> {

    private final OwnerRepository ownerRepository;

    public OwnerService(OwnerRepository ownerRepository) {
        super(ownerRepository);
        this.ownerRepository = ownerRepository;
    }

    @Override
    protected String getEntityName() {
        return "Owner";
    }

    @Override
    protected Optional<Owner> findByIdAndClinicId(UUID id, UUID clinicId) {
        return ownerRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Owner> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return ownerRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return ownerRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Transactional(readOnly = true)
    public List<Owner> searchByLastName(UUID clinicId, String lastName) {
        return ownerRepository.findByClinicIdAndDeletedFalseAndLastNameContainingIgnoreCase(clinicId, lastName);
    }

    @Transactional(readOnly = true)
    public List<Owner> searchByPhone(UUID clinicId, String phone) {
        return ownerRepository.findByClinicIdAndDeletedFalseAndPhone(clinicId, phone);
    }
    
    public Page<Owner> searchAll(UUID clinicId, String search, Pageable pageable) {
    	if (search == null || search.isBlank()) {
            return findAllByClinicId(clinicId, pageable);
        }
        return ownerRepository.searchByClinicId(clinicId, search, pageable);
    }

}
