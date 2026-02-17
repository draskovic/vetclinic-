package com.softart.vetclinic.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateVaccinationRequest(
        UUID petId,
        UUID medicalRecordId,
        UUID vetId,
        String vaccineName,
        String batchNumber,
        String manufacturer,
        OffsetDateTime administeredAt,
        LocalDate validUntil,
        LocalDate nextDueDate
) {}
