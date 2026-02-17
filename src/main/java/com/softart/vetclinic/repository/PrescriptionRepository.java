package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByMedicalRecordId(UUID medicalRecordId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Prescription> findByClinicIdAndPetId(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<Prescription> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Page<Prescription> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Prescription> findByClinicIdAndMedicalRecordIdAndDeletedFalse(UUID clinicId, UUID medicalRecordId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Prescription> findByClinicIdAndPetIdAndDeletedFalse(UUID clinicId, UUID petId);
}
