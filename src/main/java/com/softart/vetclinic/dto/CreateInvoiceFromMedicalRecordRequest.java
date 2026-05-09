package com.softart.vetclinic.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateInvoiceFromMedicalRecordRequest(
        UUID locationId,
        OffsetDateTime issuedAt,
        LocalDate dueDate,
        String currency,
        String note
) {}