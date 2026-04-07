package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Diagnosis;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.repository.DiagnosisRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class DiagnosisService extends AbstractCrudService<Diagnosis, DiagnosisRepository> {

    private final DiagnosisRepository diagnosisRepository;

    public DiagnosisService(DiagnosisRepository diagnosisRepository) {
        super(diagnosisRepository);
        this.diagnosisRepository = diagnosisRepository;
    }

    @Override
    protected String getEntityName() {
        return "Diagnosis";
    }

    @Override
    protected Optional<Diagnosis> findByIdAndClinicId(UUID id, UUID clinicId) {
        return diagnosisRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Diagnosis> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return diagnosisRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return diagnosisRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Diagnosis entity) {
        if (diagnosisRepository.existsByClinicIdAndNameAndDeletedFalse(entity.getClinicId(), entity.getName())) {
            throw new DuplicateResourceException("Diagnosis", "name", entity.getName());
        }
    }

    @Transactional(readOnly = true)
    public Page<Diagnosis> searchAll(UUID clinicId, String search, Pageable pageable) {
        String searchParam = (search == null || search.isBlank()) ? "" : search;
        return diagnosisRepository.searchByClinicId(clinicId, searchParam, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Diagnosis> searchActive(UUID clinicId, String search, Pageable pageable) {
        String searchParam = (search == null || search.isBlank()) ? "" : search;
        return diagnosisRepository.searchActiveByClinicId(clinicId, searchParam, pageable);
    }
}
