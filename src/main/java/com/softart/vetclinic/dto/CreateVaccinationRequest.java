package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateVaccinationRequest(
        @NotNull UUID petId,
        UUID medicalRecordId,
        @NotNull UUID vetId,
        @NotBlank String vaccineName,
        String batchNumber,
        String manufacturer,
        @NotNull OffsetDateTime administeredAt,
        LocalDate validUntil,
        LocalDate nextDueDate
) {}
