package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Owner;
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
public interface OwnerRepository extends JpaRepository<Owner, UUID> {

    List<Owner> findByClinicIdAndLastNameContainingIgnoreCase(UUID clinicId, String lastName);

    Optional<Owner> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Owner> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<Owner> findByClinicIdAndDeletedFalseAndLastNameContainingIgnoreCase(UUID clinicId, String lastName);

    List<Owner> findByClinicIdAndDeletedFalseAndPhone(UUID clinicId, String phone);
    
    @EntityGraph(attributePaths = {"clinic"})
    @Query("SELECT o FROM Owner o WHERE o.clinicId = :clinicId AND o.deleted = false " +
           "AND (:search IS NULL OR LOWER(o.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.address) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Owner> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);

}
