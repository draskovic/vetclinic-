package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.AuditAction;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID clinicId,
        UUID userId,
        AuditAction action,
        String entityType,
        UUID entityId,
        String oldValues,
        String newValues,
        String ipAddress,
        String userAgent,
        OffsetDateTime createdAt
) {}
