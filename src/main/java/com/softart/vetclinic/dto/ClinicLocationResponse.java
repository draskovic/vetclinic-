package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClinicLocationResponse(
        UUID id,
        String name,
        String address,
        String city,
        String phone,
        String email,
        Boolean isMain,
        Boolean active,
        String workingHours,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
