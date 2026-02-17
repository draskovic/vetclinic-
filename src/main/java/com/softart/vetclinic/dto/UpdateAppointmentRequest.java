package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.enums.AppointmentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateAppointmentRequest(
        UUID locationId,
        UUID petId,
        UUID ownerId,
        UUID vetId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        AppointmentStatus status,
        AppointmentType type,
        String reason,
        String notes,
        UUID followUpTo
) {}
