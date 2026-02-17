package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoginRequest(
        @NotNull UUID clinicId,
        @NotBlank String email,
        @NotBlank String password
) {}
