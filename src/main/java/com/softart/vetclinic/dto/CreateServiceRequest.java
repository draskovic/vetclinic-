package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotNull ServiceCategory category,
        @NotBlank String name,
        String sku,
        String unit,
        String description,
        @NotNull BigDecimal price,
        BigDecimal taxRate,
        Integer durationMinutes,
        Boolean active
) {}
