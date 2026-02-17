package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String permissions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
