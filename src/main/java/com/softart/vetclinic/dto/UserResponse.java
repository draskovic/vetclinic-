package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID roleId,
        UUID clinicId,
        String roleName,
        String firstName,
        String lastName,
        String email,
        String phone,
        String licenseNumber,
        String specialization,
        Boolean active,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
