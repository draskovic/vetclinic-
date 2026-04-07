package com.softart.vetclinic.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.Diagnosis;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    Optional<Diagnosis> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Diagnosis> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndNameAndDeletedFalse(UUID clinicId, String name);

    @Query("SELECT d FROM Diagnosis d WHERE d.clinicId = :clinicId AND d.deleted = false " +
           "AND (:search = '' OR (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.category) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Diagnosis> searchByClinicId(
        @Param("clinicId") UUID clinicId,
        @Param("search") String search,
        Pageable pageable);

    @Query("SELECT d FROM Diagnosis d WHERE d.clinicId = :clinicId AND d.deleted = false " +
           "AND d.active = true " +
           "AND (:search = '' OR (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.category) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Diagnosis> searchActiveByClinicId(
        @Param("clinicId") UUID clinicId,
        @Param("search") String search,
        Pageable pageable);
}
