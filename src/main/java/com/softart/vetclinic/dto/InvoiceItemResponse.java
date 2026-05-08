package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID invoiceId,
        UUID serviceId,
        String serviceName,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        UUID taxRateId,
        String taxRateLabel,
        BigDecimal taxRatePercent,
        BigDecimal discountPercent,
        BigDecimal lineTotal,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}