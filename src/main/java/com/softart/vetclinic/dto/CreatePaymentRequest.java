package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID invoiceId,
        @NotNull BigDecimal amount,
        @NotNull PaymentMethod method,
        @NotNull OffsetDateTime paidAt,
        String referenceNumber,
        String note
) {}
