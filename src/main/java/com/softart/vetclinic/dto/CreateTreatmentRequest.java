package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTreatmentRequest(
        @NotNull UUID medicalRecordId,
        UUID serviceId,
        @NotNull UUID vetId,
        @NotBlank String name,
        String description,
        String toothChart,
        String result,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent
) {}
