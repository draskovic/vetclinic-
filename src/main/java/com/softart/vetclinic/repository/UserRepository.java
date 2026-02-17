package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByClinicIdAndEmail(UUID clinicId, String email);

    List<User> findByClinicIdAndActiveTrue(UUID clinicId);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"role"})
    Page<User> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndEmailAndDeletedFalse(UUID clinicId, String email);
    
    List<User> findByClinicIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDeletedFalse(
            UUID clinicId, String firstName, String lastName);

}
