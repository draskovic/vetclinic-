package com.softart.vetclinic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TreatmentResponse(
        UUID id,
        UUID medicalRecordId,
        UUID serviceId,
        String serviceName,
        UUID vetId,
        String vetName,
        String name,
        String description,
        String toothChart,
        String result,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
