package com.softart.vetclinic.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PdfParseResult(
        String reportNumber,
        String petName,
        UUID petId,
        String vetName,
        UUID vetId,
        String laboratoryName,
        String analysisType,
        LocalDate requestedAt,
        LocalDate completedAt
) {}
