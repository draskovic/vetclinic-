package com.softart.vetclinic.dto;

import java.util.List;

public record UpdateBookingSettingsRequest(
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
