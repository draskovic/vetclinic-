package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.TreatmentProtocol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TreatmentProtocolRepository extends JpaRepository<TreatmentProtocol, UUID> {

    Optional<TreatmentProtocol> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<TreatmentProtocol> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndNameAndDeletedFalse(UUID clinicId, String name);

    List<TreatmentProtocol> findByClinicIdAndDiagnosisIdAndActiveTrueAndDeletedFalse(UUID clinicId, UUID diagnosisId);

    @Query("SELECT tp FROM TreatmentProtocol tp WHERE tp.clinicId = :clinicId AND tp.deleted = false " +
           "AND (:search = '' OR LOWER(tp.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TreatmentProtocol> searchByClinicId(
        @Param("clinicId") UUID clinicId,
        @Param("search") String search,
        Pageable pageable);
}
