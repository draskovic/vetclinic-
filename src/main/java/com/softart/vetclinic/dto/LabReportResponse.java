package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.LabReportStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.softart.vetclinic.enums.TestCategory;

public record LabReportResponse(
        UUID id,
        String reportNumber,
        String analysisType,
        UUID petId,
        String petName,
        String ownerName,
        UUID vetId,
        String vetName,
        UUID medicalRecordId,
        TestCategory testCategory,
        String laboratoryName,
        LabReportStatus status,
        LocalDate requestedAt,
        LocalDate completedAt,
        String resultSummary,
        Boolean isAbnormal,
        String notes,
        String fileName,
        String mimeType,
        Long fileSizeBytes,
        String storagePath,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
