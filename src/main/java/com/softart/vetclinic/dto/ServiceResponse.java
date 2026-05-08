package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        ServiceCategory category,
        String name,
        String sku,
        String unit,
        String description,
        BigDecimal price,
        UUID taxRateId,
        String taxRateLabel,
        BigDecimal taxRatePercent,
        Integer durationMinutes,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}