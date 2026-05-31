package com.softart.vetclinic.dto;

import java.util.UUID;

import com.softart.vetclinic.enums.HealthAlertType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePetHealthAlertRequest(
        @NotNull UUID petId,
        @NotNull HealthAlertType alertType,
        @NotBlank @Size(max = 200) String label,
        String description,
        Boolean active
) {}