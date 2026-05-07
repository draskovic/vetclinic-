package com.softart.vetclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.MedicationAdministration;

@Repository
public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, UUID> {

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem"})
    Optional<MedicationAdministration> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem"})
    Page<MedicationAdministration> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem"})
    List<MedicationAdministration> findByClinicIdAndMedicalRecordIdAndDeletedFalseOrderByAdministeredDateDesc(UUID clinicId, UUID medicalRecordId);

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem"})
    List<MedicationAdministration> findByClinicIdAndPetIdAndDeletedFalse(UUID clinicId, UUID petId);
}