package com.softart.vetclinic.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdatePrescriptionRequest(
        UUID medicalRecordId,
        UUID petId,
        UUID vetId,
        String medicationName,
        String dosage,
        String frequency,
        Integer durationDays,
        LocalDate startDate,
        LocalDate endDate,
        String instructions,
        UUID inventoryItemId
) {}
