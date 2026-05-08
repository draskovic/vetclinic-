package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceRequest(
        @NotNull ServiceCategory category,
        @NotBlank String name,
        String sku,
        String unit,
        String description,
        @NotNull BigDecimal price,
        UUID taxRateId,
        Integer durationMinutes,
        Boolean active
) {}