package com.softart.vetclinic.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID medicalRecordId,
        UUID petId,
        String petName,
        UUID vetId,
        String vetName,
        String medicationName,
        String dosage,
        String frequency,
        Integer durationDays,
        LocalDate startDate,
        LocalDate endDate,
        String instructions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
