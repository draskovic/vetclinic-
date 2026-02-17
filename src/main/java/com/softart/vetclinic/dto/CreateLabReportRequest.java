package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.LabReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLabReportRequest(
        @NotBlank String reportNumber,
        @NotBlank String analysisType,
        @NotNull UUID petId,
        @NotNull UUID vetId,
        UUID medicalRecordId,
        String laboratoryName,
        LabReportStatus status,
        @NotNull LocalDate requestedAt,
        LocalDate completedAt,
        String resultSummary,
        Boolean isAbnormal,
        String notes
) {}
