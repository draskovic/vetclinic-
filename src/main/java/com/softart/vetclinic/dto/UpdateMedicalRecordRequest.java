package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateMedicalRecordRequest(
        UUID appointmentId,
        UUID petId,
        UUID vetId,
        String symptoms,
        List<UUID> diagnosisIds,
        String examinationNotes,
        BigDecimal weightKg,
        BigDecimal temperatureC,
        Integer heartRate,
        Boolean followUpRecommended,
        LocalDate followUpDate
) {}
