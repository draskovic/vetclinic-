package com.softart.vetclinic.dto;

import java.util.List;
import java.util.UUID;

public record BookingSettingsResponse(
    UUID id,
    UUID clinicId,
    Boolean enabled,
    Integer slotDurationMinutes,
    Integer bufferMinutes,
    Integer maxAdvanceDays,
    List<String> allowedTypes,
    Boolean autoConfirm,
    Boolean allowVetSelection,
    Integer cancellationHours,
    String timezone
) {}
