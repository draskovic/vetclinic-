package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceId(UUID invoiceId);

    Optional<Payment> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Payment> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<Payment> findByClinicIdAndInvoiceIdAndDeletedFalse(UUID clinicId, UUID invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoiceId = :invoiceId AND p.deleted = false")
    BigDecimal sumByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
