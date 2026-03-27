package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Pet;
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
public interface PetRepository extends JpaRepository<Pet, UUID> {

    List<Pet> findByClinicIdAndOwnerId(UUID clinicId, UUID ownerId);

    @EntityGraph(attributePaths = {"owner", "species", "breed"})
    Optional<Pet> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"owner", "species", "breed"})
    Page<Pet> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"owner", "species", "breed"})
    List<Pet> findByClinicIdAndOwnerIdAndDeletedFalse(UUID clinicId, UUID ownerId);
    
    List<Pet> findByClinicIdAndNameIgnoreCaseAndDeletedFalse(UUID clinicId, String name);

    @EntityGraph(attributePaths = {"owner", "species", "breed"})
    @Query("SELECT p FROM Pet p WHERE p.clinicId = :clinicId AND p.deleted = false " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.owner.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.owner.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.species.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.patientCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.breed.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Pet> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);

    @Query("SELECT MAX(p.patientCode) FROM Pet p WHERE p.clinicId = :clinicId AND p.deleted = false AND p.patientCode LIKE :prefix")
    String findMaxPatientCodeByPrefix(@Param("clinicId") UUID clinicId, @Param("prefix") String prefix);

}
