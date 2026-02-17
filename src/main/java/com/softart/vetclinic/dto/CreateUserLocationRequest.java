package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserLocationRequest(
        @NotNull UUID userId,
        @NotNull UUID locationId,
        Boolean isPrimary
) {}
