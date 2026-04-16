package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.ClinicLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicLocationRepository extends JpaRepository<ClinicLocation, UUID> {

    List<ClinicLocation> findByClinicIdAndActiveTrue(UUID clinicId);

    Optional<ClinicLocation> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<ClinicLocation> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);
    
    List<ClinicLocation> findByClinicIdAndDeletedFalseAndActiveTrue(UUID clinicId);

}
