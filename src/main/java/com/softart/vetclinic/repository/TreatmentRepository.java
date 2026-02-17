package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Treatment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {

    List<Treatment> findByMedicalRecordId(UUID medicalRecordId);

    @EntityGraph(attributePaths = {"service", "vet"})
    Optional<Treatment> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"service", "vet"})
    Page<Treatment> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"service", "vet"})
    List<Treatment> findByClinicIdAndMedicalRecordIdAndDeletedFalse(UUID clinicId, UUID medicalRecordId);
}
