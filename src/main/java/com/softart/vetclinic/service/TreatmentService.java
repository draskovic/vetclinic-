package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Treatment;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.repository.TreatmentRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TreatmentService extends AbstractCrudService<Treatment, TreatmentRepository> {

    private final TreatmentRepository treatmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;

    public TreatmentService(TreatmentRepository treatmentRepository,
                            MedicalRecordRepository medicalRecordRepository,
                            UserRepository userRepository,
                            ServiceRepository serviceRepository) {
        super(treatmentRepository);
        this.treatmentRepository = treatmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    protected String getEntityName() {
        return "Treatment";
    }

    @Override
    protected Optional<Treatment> findByIdAndClinicId(UUID id, UUID clinicId) {
        return treatmentRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Treatment> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return treatmentRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return treatmentRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Treatment entity) {
        requireExists(
                medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getMedicalRecordId(), entity.getClinicId()),
                "MedicalRecord", "id", entity.getMedicalRecordId()
        );
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getVetId(), entity.getClinicId()),
                "User", "id", entity.getVetId()
        );
        if (entity.getServiceId() != null) {
            requireExists(
                    serviceRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), entity.getClinicId()),
                    "Service", "id", entity.getServiceId()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Treatment> findByMedicalRecord(UUID clinicId, UUID medicalRecordId) {
        return treatmentRepository.findByClinicIdAndMedicalRecordIdAndDeletedFalse(clinicId, medicalRecordId);
    }
}
