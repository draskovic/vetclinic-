package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateTreatmentProtocolRequest(
    @NotBlank String name,
    String description,
    UUID diagnosisId,
    Boolean active
) {}
