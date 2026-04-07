package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DiagnosisResponse(
    UUID id,
    String code,
    String name,
    String category,
    String description,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
