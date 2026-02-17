package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.AuditLogResponse;
import com.softart.vetclinic.enums.AuditAction;
import com.softart.vetclinic.mapper.AuditLogMapper;
import com.softart.vetclinic.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
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
        return auditLogService.findAll(clinicId, pageable).map(auditLogMapper::toResponse);
    }

    @GetMapping("/{id}")
    public AuditLogResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return auditLogMapper.toResponse(auditLogService.findById(id, clinicId));
    }

    @GetMapping("/by-entity/{entityType}/{entityId}")
    public Page<AuditLogResponse> getByEntity(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            Pageable pageable) {
        return auditLogService.findByEntity(clinicId, entityType, entityId, pageable)
                .map(auditLogMapper::toResponse);
    }

    @GetMapping("/by-user/{userId}")
    public Page<AuditLogResponse> getByUser(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID userId,
            Pageable pageable) {
        return auditLogService.findByUser(clinicId, userId, pageable)
                .map(auditLogMapper::toResponse);
    }

    @GetMapping("/by-action/{action}")
    public Page<AuditLogResponse> getByAction(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable AuditAction action,
            Pageable pageable) {
        return auditLogService.findByAction(clinicId, action, pageable)
                .map(auditLogMapper::toResponse);
    }

    @GetMapping("/by-date-range")
    public Page<AuditLogResponse> getByDateRange(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {
        return auditLogService.findByDateRange(clinicId, from, to, pageable)
                .map(auditLogMapper::toResponse);
    }
}
