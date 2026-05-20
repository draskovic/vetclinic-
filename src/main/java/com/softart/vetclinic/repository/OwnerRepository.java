package com.softart.vetclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.Owner;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {

    List<Owner> findByClinicIdAndLastNameContainingIgnoreCase(UUID clinicId, String lastName);

    Optional<Owner> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Owner> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<Owner> findByClinicIdAndDeletedFalseAndLastNameContainingIgnoreCase(UUID clinicId, String lastName);

    List<Owner> findByClinicIdAndDeletedFalseAndPhone(UUID clinicId, String phone);
    
    List<Owner> findByClinicIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDeletedFalse(
            UUID clinicId, String firstName, String lastName);
    
    @Query("SELECT o FROM Owner o WHERE o.clinicId = :clinicId AND o.deleted = false " +
    	       "AND (:search = '' OR (LOWER(o.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(o.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(CONCAT(o.firstName, ' ', o.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(o.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(o.clientCode) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Owner> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);

    @Query("SELECT MAX(o.clientCode) FROM Owner o WHERE o.clinicId = :clinicId AND o.deleted = false AND o.clientCode LIKE :prefix")
    String findMaxClientCodeByPrefix(@Param("clinicId") UUID clinicId, @Param("prefix") String prefix);

    Optional<Owner> findByClinicIdAndClientCodeAndDeletedFalse(UUID clinicId, String clientCode);

}
