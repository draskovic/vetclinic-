package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePetRequest(
        UUID ownerId,
        UUID speciesId,
        UUID breedId,
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
        String photoUrl
) {}
