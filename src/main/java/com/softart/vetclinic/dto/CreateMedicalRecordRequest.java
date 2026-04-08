package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateMedicalRecordRequest(
        UUID appointmentId,
        @NotNull UUID petId,
        @NotNull UUID vetId,
        String symptoms,
        List<UUID> diagnosisIds,
        String examinationNotes,
        BigDecimal weightKg,
        BigDecimal temperatureC,
        Integer heartRate,
        Boolean followUpRecommended,
        LocalDate followUpDate
) {}
