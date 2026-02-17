package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.enums.AppointmentType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID locationId,
        @NotNull UUID petId,
        @NotNull UUID ownerId,
        @NotNull UUID vetId,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        AppointmentStatus status,
        @NotNull AppointmentType type,
        String reason,
        String notes,
        UUID followUpTo
) {}
