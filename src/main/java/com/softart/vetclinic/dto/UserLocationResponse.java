package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserLocationResponse(
        UUID id,
        UUID userId,
        String userName,
        UUID locationId,
        String locationName,
        Boolean isPrimary,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
