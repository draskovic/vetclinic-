package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {

    List<Owner> findByClinicIdAndLastNameContainingIgnoreCase(UUID clinicId, String lastName);

    Optional<Owner> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Owner> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<Owner> findByClinicIdAndDeletedFalseAndLastNameContainingIgnoreCase(UUID clinicId, String lastName);

    List<Owner> findByClinicIdAndDeletedFalseAndPhone(UUID clinicId, String phone);
}
