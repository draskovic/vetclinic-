package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePetRequest(
        @NotNull UUID ownerId,
        UUID speciesId,
        UUID breedId,
        @NotBlank String name,
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
        String photoUrl
) {}
