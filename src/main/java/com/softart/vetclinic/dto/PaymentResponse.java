package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        BigDecimal amount,
        PaymentMethod method,
        OffsetDateTime paidAt,
        String referenceNumber,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
