package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Prescription;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.PrescriptionRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrescriptionService extends AbstractCrudService<Prescription, PrescriptionRepository> {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               PetRepository petRepository,
                               UserRepository userRepository) {
        super(prescriptionRepository);
        this.prescriptionRepository = prescriptionRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected String getEntityName() {
        return "Prescription";
    }

    @Override
    protected Optional<Prescription> findByIdAndClinicId(UUID id, UUID clinicId) {
        return prescriptionRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Prescription> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return prescriptionRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return prescriptionRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Prescription entity) {
        requireExists(
                medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getMedicalRecordId(), entity.getClinicId()),
                "MedicalRecord", "id", entity.getMedicalRecordId()
        );
        requireExists(
                petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                "Pet", "id", entity.getPetId()
        );
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getVetId(), entity.getClinicId()),
                "User", "id", entity.getVetId()
        );
    }

    @Transactional(readOnly = true)
    public List<Prescription> findByMedicalRecord(UUID clinicId, UUID medicalRecordId) {
        return prescriptionRepository.findByClinicIdAndMedicalRecordIdAndDeletedFalse(clinicId, medicalRecordId);
    }

    @Transactional(readOnly = true)
    public List<Prescription> findByPet(UUID clinicId, UUID petId) {
        return prescriptionRepository.findByClinicIdAndPetIdAndDeletedFalse(clinicId, petId);
    }
}
