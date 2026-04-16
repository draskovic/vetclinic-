package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.AppointmentStatus;

import java.util.UUID;

public record BookingCreateResponse(
    UUID appointmentId,
    AppointmentStatus status,
    String message
) {}
