package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateServiceRequest(
        ServiceCategory category,
        String name,
        String sku,
        String unit,
        String description,
        BigDecimal price,
        UUID taxRateId,
        Integer durationMinutes,
        Boolean active
) {}