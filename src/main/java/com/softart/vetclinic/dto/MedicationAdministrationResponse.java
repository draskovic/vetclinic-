package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.MedicationRoute;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicationAdministrationResponse(
        UUID id,
        UUID medicalRecordId,
        UUID petId,
        String petName,
        UUID vetId,
        String vetName,
        UUID inventoryItemId,
        String inventoryItemName,
        String medicationName,
        String dosage,
        MedicationRoute route,
        LocalDate administeredDate,
        String instructions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}