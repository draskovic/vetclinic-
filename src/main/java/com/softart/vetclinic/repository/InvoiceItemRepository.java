package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.InvoiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByInvoiceIdOrderBySortOrderAsc(UUID invoiceId);

    @EntityGraph(attributePaths = {"service"})
    Optional<InvoiceItem> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"service"})
    Page<InvoiceItem> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"service"})
    List<InvoiceItem> findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(UUID clinicId, UUID invoiceId);
}
