package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.MedicationRoute;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateMedicationAdministrationRequest(
        UUID medicalRecordId,
        UUID petId,
        UUID vetId,
        UUID inventoryItemId,
        String medicationName,
        String dosage,
        MedicationRoute route,
        LocalDate administeredDate,
        String instructions
) {}