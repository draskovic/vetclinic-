package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInvoiceItemRequest(
        @NotNull UUID invoiceId,
        UUID serviceId,
        @NotBlank String description,
        BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        UUID taxRateId,
        BigDecimal discountPercent,
        @NotNull BigDecimal lineTotal,
        Integer sortOrder
) {}