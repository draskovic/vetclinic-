package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BreedResponse(
        UUID id,
        UUID speciesId,
        String speciesName,
        String name,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
