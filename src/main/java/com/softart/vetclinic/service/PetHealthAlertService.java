package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.PetHealthAlert;
import com.softart.vetclinic.repository.PetHealthAlertRepository;
import com.softart.vetclinic.repository.PetRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PetHealthAlertService extends AbstractCrudService<PetHealthAlert, PetHealthAlertRepository> {

    private final PetHealthAlertRepository repository;
    private final PetRepository petRepository;

    public PetHealthAlertService(PetHealthAlertRepository repository,
                                 PetRepository petRepository) {
        super(repository);
        this.repository = repository;
        this.petRepository = petRepository;
    }

    @Override
    protected String getEntityName() {
        return "PetHealthAlert";
    }

    @Override
    protected Optional<PetHealthAlert> findByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<PetHealthAlert> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return repository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(PetHealthAlert entity) {
        requireExists(
                petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                "Pet", "id", entity.getPetId()
        );
    }

    /**
     * Lista SAMO aktivnih alert-a za pet-a — koristi se za banner u 4 UI lokacije
     * (MedicalRecordEditor, PetProfilePage, AppointmentModal, MedicalRecordsPage ikonica).
     */
    @Transactional(readOnly = true)
    public List<PetHealthAlert> findActiveByPet(UUID clinicId, UUID petId) {
        return repository.findByClinicIdAndPetIdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(clinicId, petId);
    }

    /**
     * Lista SVIH alert-a za pet-a (uključuje deaktivirane sa active=false) —
     * koristi se u editor modal-u gde vet sme da reaktivira preko Switch-a.
     */
    @Transactional(readOnly = true)
    public List<PetHealthAlert> findAllByPet(UUID clinicId, UUID petId) {
        return repository.findByClinicIdAndPetIdAndDeletedFalseOrderByCreatedAtDesc(clinicId, petId);
    }

    /**
     * Batch enrich helper za MedicalRecordController.list — koji pet-ovi iz date
     * stranice imaju bar jedan active alert. 1 query za celu stranicu.
     */
    @Transactional(readOnly = true)
    public Set<UUID> findPetIdsWithActiveAlerts(UUID clinicId, Set<UUID> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            return Collections.emptySet();
        }
        return repository.findPetIdsWithActiveAlerts(clinicId, petIds);
    }
}