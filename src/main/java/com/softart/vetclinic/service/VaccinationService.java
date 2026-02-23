package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Vaccination;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import com.softart.vetclinic.repository.VaccinationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VaccinationService extends AbstractCrudService<Vaccination, VaccinationRepository> {

    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    public VaccinationService(VaccinationRepository vaccinationRepository,
                              PetRepository petRepository,
                              UserRepository userRepository,
                              MedicalRecordRepository medicalRecordRepository) {
        super(vaccinationRepository);
        this.vaccinationRepository = vaccinationRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    protected String getEntityName() {
        return "Vaccination";
    }

    @Override
    protected Optional<Vaccination> findByIdAndClinicId(UUID id, UUID clinicId) {
        return vaccinationRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Vaccination> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return vaccinationRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return vaccinationRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Vaccination entity) {
        requireExists(
                petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                "Pet", "id", entity.getPetId()
        );
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getVetId(), entity.getClinicId()),
                "User", "id", entity.getVetId()
        );
        if (entity.getMedicalRecordId() != null) {
            requireExists(
                    medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getMedicalRecordId(), entity.getClinicId()),
                    "MedicalRecord", "id", entity.getMedicalRecordId()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Vaccination> findByPet(UUID clinicId, UUID petId) {
        return vaccinationRepository.findByClinicIdAndPetIdAndDeletedFalse(clinicId, petId);
    }

    @Transactional(readOnly = true)
    public List<Vaccination> findDueVaccinations(UUID clinicId, LocalDate beforeDate) {
        return vaccinationRepository.findByClinicIdAndDeletedFalseAndNextDueDateBefore(clinicId, beforeDate);
    }
    
    public List<Vaccination> findByMedicalRecord(UUID clinicId, UUID medicalRecordId) {
        return vaccinationRepository.findByClinicIdAndMedicalRecordIdAndDeletedFalse(clinicId, medicalRecordId);
    }

}
