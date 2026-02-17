package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.enums.InvoiceStatus;
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
}
