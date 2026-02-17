package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePrescriptionRequest(
        @NotNull UUID medicalRecordId,
        @NotNull UUID petId,
        @NotNull UUID vetId,
        @NotBlank String medicationName,
        @NotBlank String dosage,
        @NotBlank String frequency,
        Integer durationDays,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        String instructions
) {}
