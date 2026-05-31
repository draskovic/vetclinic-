package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PetResponse(
        UUID id,
        UUID ownerId,
        String ownerName,
        UUID speciesId,
        String speciesName,
        UUID breedId,
        String breedName,
        String name,
        LocalDate dateOfBirth,
        Gender gender,
        String color,
        BigDecimal weightKg,
        String microchipNumber,
        Boolean isNeutered,
        Boolean isDeceased,
        LocalDate deceasedAt,
        String allergies,
        String note,
        String photoUrl,
        String patientCode,
        String legacyCode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Boolean hasActiveAlerts
) {}
