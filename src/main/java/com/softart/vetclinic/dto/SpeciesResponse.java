package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SpeciesResponse(
        UUID id,
        String name,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
