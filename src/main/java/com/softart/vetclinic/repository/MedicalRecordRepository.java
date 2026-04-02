package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.MedicalRecord;
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
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    List<MedicalRecord> findByClinicIdAndPetIdOrderByCreatedAtDesc(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<MedicalRecord> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Page<MedicalRecord> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<MedicalRecord> findByClinicIdAndPetIdAndDeletedFalseOrderByCreatedAtDesc(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<MedicalRecord> findByAppointmentIdAndDeletedFalse(UUID appointmentId);
    
  

    @Query("SELECT mr FROM MedicalRecord mr JOIN Pet p ON mr.petId = p.id " +
    	       "WHERE mr.clinicId = :clinicId AND p.ownerId = :ownerId AND mr.deleted = false " +
    	       "ORDER BY mr.createdAt DESC")
    	List<MedicalRecord> findByClinicIdAndOwnerIdOrderByCreatedAtDesc(
    	    @Param("clinicId") UUID clinicId, 
    	    @Param("ownerId") UUID ownerId);


    @EntityGraph(attributePaths = {"pet", "vet"})
    @Query("SELECT m FROM MedicalRecord m WHERE m.clinicId = :clinicId AND m.deleted = false " +
           "AND (:search = '' OR (LOWER(m.pet.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.diagnosis) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.symptoms) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.vet.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.vet.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.recordCode) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "OR LOWER(CONCAT(m.vet.firstName, ' ', m.vet.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))))")

    Page<MedicalRecord> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT MAX(m.recordCode) FROM MedicalRecord m WHERE m.clinicId = :clinicId AND m.recordCode LIKE :prefix% AND m.deleted = false")
    String findMaxRecordCodeByPrefix(@Param("clinicId") UUID clinicId, @Param("prefix") String prefix);

}
