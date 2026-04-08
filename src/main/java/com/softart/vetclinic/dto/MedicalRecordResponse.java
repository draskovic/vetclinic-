package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MedicalRecordResponse(
        UUID id,
        UUID appointmentId,
        String recordCode,
        UUID petId,
        String petName,
        UUID ownerId,
        String ownerName,
        UUID vetId,
        String vetName,
        String symptoms,
        List<DiagnosisResponse> diagnoses,
        String examinationNotes,
        BigDecimal weightKg,
        BigDecimal temperatureC,
        Integer heartRate,
        Boolean followUpRecommended,
        LocalDate followUpDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
