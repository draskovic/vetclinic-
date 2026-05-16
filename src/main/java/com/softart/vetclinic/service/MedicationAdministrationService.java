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
import com.softart.vetclinic.dto.MedicationQuickPickItem;
import com.softart.vetclinic.dto.MedicationQuickPicksResponse;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.InventoryItemRepository;

import java.time.LocalDate;
import java.util.Objects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MedicationAdministrationService extends AbstractCrudService<MedicationAdministration, MedicationAdministrationRepository> {

    private final MedicationAdministrationRepository repository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public MedicationAdministrationService(MedicationAdministrationRepository repository,
                                           MedicalRecordRepository medicalRecordRepository,
                                           PetRepository petRepository,
                                           UserRepository userRepository,
                                           InventoryItemRepository inventoryItemRepository) {
        super(repository);
        this.repository = repository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.inventoryItemRepository = inventoryItemRepository;
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
    	return repository.findByClinicIdAndPetIdAndDeletedFalseOrderByAdministeredDateDesc(clinicId, petId);
    }
    
    @Transactional
    public List<MedicationAdministration> createBulk(
            List<MedicationAdministration> entities, UUID clinicId) {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("Lista ne sme biti prazna");
        }

        // Distinct FK validacija — 3 query-ja umesto N×3
        entities.stream()
                .map(MedicationAdministration::getMedicalRecordId)
                .distinct()
                .forEach(id -> requireExists(
                        medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId),
                        "MedicalRecord", "id", id));
        entities.stream()
                .map(MedicationAdministration::getPetId)
                .distinct()
                .forEach(id -> requireExists(
                        petRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId),
                        "Pet", "id", id));
        entities.stream()
                .map(MedicationAdministration::getVetId)
                .distinct()
                .forEach(id -> requireExists(
                        userRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId),
                        "User", "id", id));

        // InventoryItem validacija (jedan query za distinct ne-null IDs)
        List<UUID> itemIds = entities.stream()
                .map(MedicationAdministration::getInventoryItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!itemIds.isEmpty()) {
            List<UUID> found = inventoryItemRepository.findExistingIdsByIdsAndClinicId(itemIds, clinicId);
            if (found.size() != itemIds.size()) {
                throw new ResourceNotFoundException(
                        "InventoryItem", "id", "neki ID iz batch-a ne postoji u klinici");
            }
        }

        entities.forEach(e -> {
            e.setClinicId(clinicId);
            e.setDeleted(false);
        });
        return repository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public MedicationQuickPicksResponse getQuickPicks(UUID clinicId, int limit) {
        var recent = repository.findRecentDistinctMedications(clinicId, limit).stream()
                .map(this::toQuickPickItem)
                .toList();
        var frequent = repository.findFrequentMedications(
                clinicId, limit, LocalDate.now().minusDays(30)).stream()
                .map(this::toQuickPickItem)
                .toList();
        return new MedicationQuickPicksResponse(recent, frequent);
    }

    private MedicationQuickPickItem toQuickPickItem(
            MedicationAdministrationRepository.MedicationQuickPickRow row) {
        return new MedicationQuickPickItem(
                row.getInventoryItemId(),
                row.getName(),
                row.getQuantityOnHand(),
                row.getUnit());
    }
}