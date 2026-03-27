package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Document;
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
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @EntityGraph(attributePaths = {"pet", "uploadedByUser"})
    List<Document> findByClinicIdAndPetId(UUID clinicId, UUID petId);

    List<Document> findByMedicalRecordId(UUID medicalRecordId);

    @EntityGraph(attributePaths = {"pet", "uploadedByUser"})
    Optional<Document> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "uploadedByUser"})
    Page<Document> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "uploadedByUser"})
    List<Document> findByClinicIdAndPetIdAndDeletedFalse(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "uploadedByUser"})
    List<Document> findByClinicIdAndMedicalRecordIdAndDeletedFalse(UUID clinicId, UUID medicalRecordId);
    
    @EntityGraph(attributePaths = {"pet", "uploadedByUser"})
    @Query("SELECT d FROM Document d WHERE d.clinicId = :clinicId AND d.deleted = false " +
           "AND (:search = '' OR (LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.pet.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.uploadedByUser.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.uploadedByUser.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(CONCAT(d.uploadedByUser.firstName, ' ', d.uploadedByUser.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Document> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);

}
