package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.enums.AppointmentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID locationId,
        String locationName,
        UUID petId,
        String petName,
        UUID ownerId,
        String ownerName,
        UUID vetId,
        String vetName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        AppointmentStatus status,
        AppointmentType type,
        String reason,
        String notes,
        UUID followUpTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
