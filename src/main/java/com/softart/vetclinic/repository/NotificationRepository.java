package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByClinicIdAndStatusAndScheduledAtBefore(UUID clinicId, NotificationStatus status, OffsetDateTime before);

    Optional<Notification> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Notification> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<Notification> findByClinicIdAndDeletedFalseAndStatusAndScheduledAtBefore(UUID clinicId, NotificationStatus status, OffsetDateTime before);
}
