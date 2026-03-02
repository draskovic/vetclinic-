package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.LabReport;
import com.softart.vetclinic.enums.LabReportStatus;
import com.softart.vetclinic.enums.TestCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    List<LabReport> findByClinicIdAndMedicalRecordIdAndDeletedFalse(UUID clinicId, UUID medicalRecordId);
    
    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    List<LabReport> findByClinicIdAndTestCategoryAndDeletedFalse(UUID clinicId, TestCategory testCategory);

    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    @Query("SELECT l FROM LabReport l WHERE l.clinicId = :clinicId AND l.deleted = false " +
           "AND (:search IS NULL OR LOWER(l.reportNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.analysisType) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.pet.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.pet.owner.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.pet.owner.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.vet.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.vet.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(l.laboratoryName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LabReport> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);

}
