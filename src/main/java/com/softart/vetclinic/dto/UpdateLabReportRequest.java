package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.LabReportStatus;
import com.softart.vetclinic.enums.TestCategory;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateLabReportRequest(
        String reportNumber,
        String analysisType,
        UUID petId,
        UUID vetId,
        UUID medicalRecordId,
        TestCategory testCategory,
        String laboratoryName,
        LabReportStatus status,
        LocalDate requestedAt,
        LocalDate completedAt,
        String resultSummary,
        Boolean isAbnormal,
        String notes
) {}
