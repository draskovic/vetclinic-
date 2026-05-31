package com.softart.vetclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.PetHealthAlert;

@Repository
public interface PetHealthAlertRepository extends JpaRepository<PetHealthAlert, UUID> {

    @EntityGraph(attributePaths = {"pet"})
    Optional<PetHealthAlert> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    // Sve alert-e za pet-a (uključuje deactivated) — za editor modal sa Switch-em "active"
    @EntityGraph(attributePaths = {"pet"})
    List<PetHealthAlert> findByClinicIdAndPetIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID clinicId, UUID petId);

    // Samo aktivni alert-i — za banner u editor-ima
    @EntityGraph(attributePaths = {"pet"})
    List<PetHealthAlert> findByClinicIdAndPetIdAndActiveTrueAndDeletedFalseOrderByCreatedAtDesc(
            UUID clinicId, UUID petId);

    /**
     * Batch enrich za MedicalRecordController: koji pet-ovi iz date liste imaju
     * bar jedan aktivan alert. 1 query za celu stranicu (ne N upita po row-u).
     *
     * Caller MORA da osigura non-empty petIds — JPQL `IN :empty` puca u nekim
     * verzijama Hibernate-a. Defensive check na strani servisa.
     */
    @Query("SELECT DISTINCT a.petId FROM PetHealthAlert a " +
           "WHERE a.clinicId = :clinicId " +
           "  AND a.petId IN :petIds " +
           "  AND a.active = true " +
           "  AND a.deleted = false")
    Set<UUID> findPetIdsWithActiveAlerts(
            @Param("clinicId") UUID clinicId,
            @Param("petIds") Set<UUID> petIds);
}