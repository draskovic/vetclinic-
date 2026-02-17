package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID appointmentId,
        UUID ownerId,
        String ownerName,
        UUID locationId,
        String locationName,
        String invoiceNumber,
        InvoiceStatus status,
        OffsetDateTime issuedAt,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal total,
        String currency,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
