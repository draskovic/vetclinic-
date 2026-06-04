package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateTreatmentRequest(
        UUID medicalRecordId,
        UUID serviceId,
        UUID vetId,
        String name,
        String description,
        String toothChart,
        String result,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent
) {}
