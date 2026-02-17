package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByClinicIdAndName(UUID clinicId, String name);

    Optional<Role> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Role> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndNameAndDeletedFalse(UUID clinicId, String name);
}
