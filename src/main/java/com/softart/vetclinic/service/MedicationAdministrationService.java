package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.MedicationAdministration;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.MedicationAdministrationRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MedicationAdministrationService extends AbstractCrudService<MedicationAdministration, MedicationAdministrationRepository> {

    private final MedicationAdministrationRepository repository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public MedicationAdministrationService(MedicationAdministrationRepository repository,
                                           MedicalRecordRepository medicalRecordRepository,
                                           PetRepository petRepository,
                                           UserRepository userRepository) {
        super(repository);
        this.repository = repository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected String getEntityName() {
        return "MedicationAdministration";
    }

    @Override
    protected Optional<MedicationAdministration> findByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<MedicationAdministration> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return repository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(MedicationAdministration entity) {
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
    public List<MedicationAdministration> findByMedicalRecord(UUID clinicId, UUID medicalRecordId) {
        return repository.findByClinicIdAndMedicalRecordIdAndDeletedFalseOrderByAdministeredDateDesc(clinicId, medicalRecordId);
    }

    @Transactional(readOnly = true)
    public List<MedicationAdministration> findByPet(UUID clinicId, UUID petId) {
        return repository.findByClinicIdAndPetIdAndDeletedFalse(clinicId, petId);
    }
}