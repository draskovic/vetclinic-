package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSpeciesRequest(
        @NotBlank String name,
        Boolean active
) {}
