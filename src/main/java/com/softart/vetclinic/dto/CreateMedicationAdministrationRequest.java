package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.MedicationRoute;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateMedicationAdministrationRequest(
        @NotNull UUID medicalRecordId,
        @NotNull UUID petId,
        @NotNull UUID vetId,
        UUID inventoryItemId,
        @NotBlank String medicationName,
        @NotBlank String dosage,
        MedicationRoute route,
        @NotNull LocalDate administeredDate,
        String instructions
) {}