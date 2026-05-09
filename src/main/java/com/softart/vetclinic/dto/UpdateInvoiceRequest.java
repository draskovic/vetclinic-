package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateInvoiceRequest(
        UUID appointmentId,
        UUID ownerId,
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
        Integer version
) {}
