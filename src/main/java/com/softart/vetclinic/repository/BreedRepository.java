package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Breed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BreedRepository extends JpaRepository<Breed, UUID> {

    List<Breed> findBySpeciesId(UUID speciesId);

    @EntityGraph(attributePaths = {"species"})
    Optional<Breed> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"species"})
    Page<Breed> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"species"})
    List<Breed> findByClinicIdAndSpeciesIdAndDeletedFalse(UUID clinicId, UUID speciesId);

    boolean existsBySpeciesIdAndNameAndDeletedFalse(UUID speciesId, String name);
    
    Optional<Breed> findBySpeciesIdAndNameIgnoreCaseAndDeletedFalse(UUID speciesId, String name);

}
