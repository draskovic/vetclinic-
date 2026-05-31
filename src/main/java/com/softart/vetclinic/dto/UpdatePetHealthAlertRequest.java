package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.HealthAlertType;

import jakarta.validation.constraints.Size;

public record UpdatePetHealthAlertRequest(
        HealthAlertType alertType,
        @Size(max = 200) String label,
        String description,
        Boolean active
) {}