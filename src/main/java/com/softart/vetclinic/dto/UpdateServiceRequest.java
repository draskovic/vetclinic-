package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;

import java.math.BigDecimal;

public record UpdateServiceRequest(
        ServiceCategory category,
        String name,
        String sku,
        String unit,
        String description,
        BigDecimal price,
        BigDecimal taxRate,
        Integer durationMinutes,
        Boolean active
) {}
