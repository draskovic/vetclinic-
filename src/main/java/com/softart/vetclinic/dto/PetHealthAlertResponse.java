package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.softart.vetclinic.enums.HealthAlertType;

public record PetHealthAlertResponse(
        UUID id,
        UUID petId,
        String petName,
        HealthAlertType alertType,
        String label,
        String description,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}