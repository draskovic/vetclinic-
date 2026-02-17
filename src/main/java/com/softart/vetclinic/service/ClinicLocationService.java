package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.ClinicLocation;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClinicLocationService extends AbstractCrudService<ClinicLocation, ClinicLocationRepository> {

    private final ClinicLocationRepository clinicLocationRepository;

    public ClinicLocationService(ClinicLocationRepository clinicLocationRepository) {
        super(clinicLocationRepository);
        this.clinicLocationRepository = clinicLocationRepository;
    }

    @Override
    protected String getEntityName() {
        return "ClinicLocation";
    }

    @Override
    protected Optional<ClinicLocation> findByIdAndClinicId(UUID id, UUID clinicId) {
        return clinicLocationRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<ClinicLocation> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return clinicLocationRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return clinicLocationRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Transactional(readOnly = true)
    public List<ClinicLocation> findActiveByClinic(UUID clinicId) {
        return clinicLocationRepository.findByClinicIdAndActiveTrue(clinicId);
    }
}
