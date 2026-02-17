package com.softart.vetclinic.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VaccinationResponse(
        UUID id,
        UUID petId,
        String petName,
        UUID medicalRecordId,
        UUID vetId,
        String vetName,
        String vaccineName,
        String batchNumber,
        String manufacturer,
        OffsetDateTime administeredAt,
        LocalDate validUntil,
        LocalDate nextDueDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
