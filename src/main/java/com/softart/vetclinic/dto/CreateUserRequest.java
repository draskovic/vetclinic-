package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserRequest(
        @NotNull UUID roleId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String email,
        @NotBlank String password,
        String phone,
        String licenseNumber,
        String specialization,
        Boolean active
) {}
