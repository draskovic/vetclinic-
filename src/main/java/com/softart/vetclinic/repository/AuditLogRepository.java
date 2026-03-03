package com.softart.vetclinic.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.AuditLog;
import com.softart.vetclinic.enums.AuditAction;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByClinicIdAndEntityTypeAndEntityId(UUID clinicId, String entityType, UUID entityId);
    
    Page<AuditLog> findByClinicIdOrderByCreatedAtDesc(UUID clinicId, Pageable pageable);

    Optional<AuditLog> findByIdAndClinicId(UUID id, UUID clinicId);
    
    Page<AuditLog> findByClinicIdAndUserIdOrderByCreatedAtDesc(UUID clinicId, UUID userId, Pageable pageable);
   
    Page<AuditLog> findByClinicIdAndActionOrderByCreatedAtDesc(UUID clinicId, AuditAction action, Pageable pageable);
    
    Page<AuditLog> findByClinicIdAndCreatedAtBetweenOrderByCreatedAtDesc(UUID clinicId, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    Page<AuditLog> findByClinicIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(UUID clinicId, String entityType, UUID entityId, Pageable pageable);
}

