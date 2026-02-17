package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
