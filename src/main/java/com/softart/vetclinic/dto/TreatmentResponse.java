package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TreatmentResponse(
        UUID id,
        UUID medicalRecordId,
        UUID serviceId,
        String serviceName,
        UUID vetId,
        String vetName,
        String name,
        String description,
        String toothChart,
        String result,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
