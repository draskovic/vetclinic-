package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxRateResponse(
        UUID id,
        String countryCode,
        String label,
        BigDecimal percent,
        String description,
        Boolean active
) {}