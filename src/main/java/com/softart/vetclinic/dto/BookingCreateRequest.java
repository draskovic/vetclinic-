package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingCreateRequest(
    @NotBlank String phone,
    String firstName,
    String lastName,
    String email,
    UUID petId,
    String petName,
    String speciesName,
    @NotNull UUID locationId,
    @NotNull AppointmentType type,
    @NotNull OffsetDateTime startTime,
    UUID preferredVetId,
    String reason,
    String honeypot
) {}
