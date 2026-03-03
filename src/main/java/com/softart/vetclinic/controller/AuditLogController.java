package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.AuditLogResponse;
import com.softart.vetclinic.entity.AuditLog;
import com.softart.vetclinic.enums.AuditAction;
import com.softart.vetclinic.mapper.AuditLogMapper;
import com.softart.vetclinic.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping
    public Page<AuditLogResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findAll(clinicId, pageable);
        return mapWithUserNames(logs);
    }

    @GetMapping("/{id}")
    public AuditLogResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        AuditLog log = auditLogService.findById(id, clinicId);
        Map<UUID, String> names = auditLogService.resolveUserNames(java.util.List.of(log));
        return withUserName(auditLogMapper.toResponse(log), names);
    }

    @GetMapping("/by-entity/{entityType}/{entityId}")
    public Page<AuditLogResponse> getByEntity(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findByEntity(clinicId, entityType, entityId, pageable);
        return mapWithUserNames(logs);
    }

    @GetMapping("/by-user/{userId}")
    public Page<AuditLogResponse> getByUser(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID userId,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findByUser(clinicId, userId, pageable);
        return mapWithUserNames(logs);
    }

    @GetMapping("/by-action/{action}")
    public Page<AuditLogResponse> getByAction(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable AuditAction action,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findByAction(clinicId, action, pageable);
        return mapWithUserNames(logs);
    }

    @GetMapping("/by-date-range")
    public Page<AuditLogResponse> getByDateRange(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findByDateRange(clinicId, from, to, pageable);
        return mapWithUserNames(logs);
    }

    // ========================================================================
    // Helper — resolve userName from userId
    // ========================================================================

    private Page<AuditLogResponse> mapWithUserNames(Page<AuditLog> logs) {
        Map<UUID, String> names = auditLogService.resolveUserNames(logs.getContent());
        return logs.map(log -> withUserName(auditLogMapper.toResponse(log), names));
    }

    private AuditLogResponse withUserName(AuditLogResponse response, Map<UUID, String> names) {
        return new AuditLogResponse(
                response.id(), response.clinicId(), response.userId(),
                names.getOrDefault(response.userId(), null),
                response.action(), response.entityType(), response.entityId(),
                response.oldValues(), response.newValues(),
                response.ipAddress(), response.userAgent(), response.createdAt()
        );
    }
}
