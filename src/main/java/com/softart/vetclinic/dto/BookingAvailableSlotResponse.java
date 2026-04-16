package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingAvailableSlotResponse(
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    UUID vetId,
    String vetName
) {}
