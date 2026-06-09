package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InvoiceStatus;

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
        OffsetDateTime updatedAt,
        Boolean hasActiveAlerts,
        BigDecimal invoiceTotal,
        InvoiceStatus invoiceStatus
) {
    // Backward-compat: postojeći pozivi bez invoice polja → invoiceTotal/invoiceStatus = null
    public MedicalRecordResponse(
            UUID id, UUID appointmentId, String recordCode, UUID petId, String petName,
            UUID ownerId, String ownerName, UUID vetId, String vetName, String symptoms,
            List<DiagnosisResponse> diagnoses, String examinationNotes, BigDecimal weightKg,
            BigDecimal temperatureC, Integer heartRate, Boolean followUpRecommended,
            LocalDate followUpDate, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            Boolean hasActiveAlerts) {
        this(id, appointmentId, recordCode, petId, petName, ownerId, ownerName, vetId, vetName,
                symptoms, diagnoses, examinationNotes, weightKg, temperatureC, heartRate,
                followUpRecommended, followUpDate, createdAt, updatedAt, hasActiveAlerts, null, null);
    }
}