package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.LabReport;
import com.softart.vetclinic.enums.LabReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabReportRepository extends JpaRepository<LabReport, UUID> {

    @EntityGraph(attributePaths = {"pet",  "pet.owner",  "vet"})
    Optional<LabReport> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "pet.owner",  "vet"})
    Page<LabReport> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet",  "pet.owner",  "vet"})
    List<LabReport> findByClinicIdAndPetIdAndDeletedFalse(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet",  "pet.owner",  "vet"})
    List<LabReport> findByClinicIdAndStatusAndDeletedFalse(UUID clinicId, LabReportStatus status);

    @EntityGraph(attributePaths = {"pet",  "pet.owner",  "vet"})
    List<LabReport> findByClinicIdAndVetIdAndDeletedFalse(UUID clinicId, UUID vetId);

    boolean existsByClinicIdAndReportNumberAndDeletedFalse(UUID clinicId, String reportNumber);
}
