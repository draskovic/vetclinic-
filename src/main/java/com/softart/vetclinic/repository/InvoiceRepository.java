package com.softart.vetclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.enums.InvoiceStatus;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @EntityGraph(attributePaths = {"location", "owner"})
    List<Invoice> findByClinicIdAndOwnerId(UUID clinicId, UUID ownerId);

    @EntityGraph(attributePaths = {"location", "owner"})
    List<Invoice> findByClinicIdAndStatus(UUID clinicId, InvoiceStatus status);

    @EntityGraph(attributePaths = {"location", "owner"})
    Optional<Invoice> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location", "owner"})
    Page<Invoice> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location", "owner"})
    List<Invoice> findByClinicIdAndOwnerIdAndDeletedFalse(UUID clinicId, UUID ownerId);

    @EntityGraph(attributePaths = {"location", "owner"})
    List<Invoice> findByClinicIdAndStatusAndDeletedFalse(UUID clinicId, InvoiceStatus status);

    boolean existsByClinicIdAndInvoiceNumberAndDeletedFalse(UUID clinicId, String invoiceNumber);

    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i WHERE i.clinicId = :clinicId")
    String findMaxInvoiceNumber(@Param("clinicId") UUID clinicId);
    
   

    @EntityGraph(attributePaths = {"location", "owner"})
    @Query("SELECT i FROM Invoice i WHERE i.clinicId = :clinicId AND i.deleted = false " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:search = '' OR (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.owner.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.owner.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(CONCAT(i.owner.firstName, ' ', i.owner.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.owner.email) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Invoice> searchByClinicIdAndStatus(
            @Param("clinicId") UUID clinicId, 
            @Param("search") String search, 
            @Param("status") InvoiceStatus status, 
            Pageable pageable);


    Optional<Invoice> findByMedicalRecordIdAndDeletedFalse(UUID medicalRecordId);

}
