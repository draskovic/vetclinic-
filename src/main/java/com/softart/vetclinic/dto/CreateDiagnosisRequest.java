package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDiagnosisRequest(
    String code,
    @NotBlank String name,
    String category,
    String description,
    Boolean active
) {}
