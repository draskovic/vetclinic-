package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Species;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, UUID> {

    List<Species> findByClinicIdAndActiveTrue(UUID clinicId);

    Optional<Species> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Species> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndNameAndDeletedFalse(UUID clinicId, String name);
    Optional<Species> findByClinicIdAndNameIgnoreCaseAndDeletedFalse(UUID clinicId, String name);

}
