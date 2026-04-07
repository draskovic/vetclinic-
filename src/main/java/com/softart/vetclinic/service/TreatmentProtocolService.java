package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.TreatmentProtocol;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.repository.TreatmentProtocolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class TreatmentProtocolService extends AbstractCrudService<TreatmentProtocol, TreatmentProtocolRepository> {

    private final TreatmentProtocolRepository protocolRepository;

    public TreatmentProtocolService(TreatmentProtocolRepository protocolRepository) {
        super(protocolRepository);
        this.protocolRepository = protocolRepository;
    }

    @Override
    protected String getEntityName() {
        return "TreatmentProtocol";
    }

    @Override
    protected Optional<TreatmentProtocol> findByIdAndClinicId(UUID id, UUID clinicId) {
        return protocolRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<TreatmentProtocol> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return protocolRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return protocolRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(TreatmentProtocol entity) {
        if (protocolRepository.existsByClinicIdAndNameAndDeletedFalse(entity.getClinicId(), entity.getName())) {
            throw new DuplicateResourceException("TreatmentProtocol", "name", entity.getName());
        }
    }

    @Transactional(readOnly = true)
    public Page<TreatmentProtocol> searchAll(UUID clinicId, String search, Pageable pageable) {
        String searchParam = (search == null || search.isBlank()) ? "" : search;
        return protocolRepository.searchByClinicId(clinicId, searchParam, pageable);
    }

    @Transactional(readOnly = true)
    public List<TreatmentProtocol> findActiveByDiagnosis(UUID clinicId, UUID diagnosisId) {
        return protocolRepository.findByClinicIdAndDiagnosisIdAndActiveTrueAndDeletedFalse(clinicId, diagnosisId);
    }
}
