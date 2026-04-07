package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TreatmentProtocolResponse(
    UUID id,
    String name,
    String description,
    UUID diagnosisId,
    String diagnosisName,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
