package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.AuditLog;
import com.softart.vetclinic.enums.AuditAction;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ========================================================================
    // Write (poziva AOP aspekt)
    // ========================================================================

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(UUID clinicId, UUID userId, AuditAction action,
                         String entityType, UUID entityId,
                         String oldValues, String newValues,
                         String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setClinicId(clinicId);
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setOldValues(oldValues);
            auditLog.setNewValues(newValues);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }

    // ========================================================================
    // Read (poziva controller)
    // ========================================================================

    @Transactional(readOnly = true)
    public AuditLog findById(UUID id, UUID clinicId) {
        return auditLogRepository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(UUID clinicId, Pageable pageable) {
        return auditLogRepository.findByClinicIdOrderByCreatedAtDesc(clinicId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntity(UUID clinicId, String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByClinicIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                clinicId, entityType, entityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByUser(UUID clinicId, UUID userId, Pageable pageable) {
        return auditLogRepository.findByClinicIdAndUserIdOrderByCreatedAtDesc(clinicId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByAction(UUID clinicId, AuditAction action, Pageable pageable) {
        return auditLogRepository.findByClinicIdAndActionOrderByCreatedAtDesc(clinicId, action, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByDateRange(UUID clinicId, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return auditLogRepository.findByClinicIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                clinicId, from, to, pageable);
    }
}
