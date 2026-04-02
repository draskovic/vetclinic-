package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.ServiceCategory;
import java.math.BigDecimal;

public record ImportServiceRequest(
        String sku,
        String name,
        BigDecimal price,
        String unit,
        ServiceCategory category
) {}
