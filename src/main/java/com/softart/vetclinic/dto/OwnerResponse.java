package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String city,
        String personalId,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
