package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.softart.vetclinic.enums.InvoiceStatus;

public record CreateInvoiceRequest(
        UUID appointmentId,
        UUID ownerId,
        UUID medicalRecordId,
        UUID locationId,
        InvoiceStatus status,
        OffsetDateTime issuedAt,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal total,
        String currency,
        String note,
        String walkInCustomerName
) {}
