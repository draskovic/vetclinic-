package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateBreedRequest(
        @NotNull UUID speciesId,
        @NotBlank String name
) {}
