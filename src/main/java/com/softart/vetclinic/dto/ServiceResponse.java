package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        ServiceCategory category,
        String name,
        String description,
        BigDecimal price,
        BigDecimal taxRate,
        Integer durationMinutes,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
