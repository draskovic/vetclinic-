package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTreatmentRequest(
        @NotNull UUID medicalRecordId,
        UUID serviceId,
        @NotNull UUID vetId,
        @NotBlank String name,
        String description,
        String toothChart,
        String result
) {}
