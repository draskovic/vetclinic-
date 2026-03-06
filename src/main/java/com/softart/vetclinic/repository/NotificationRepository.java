package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.softart.vetclinic.enums.NotificationChannel;


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
    
    Page<Notification> findByClinicIdAndRecipientIdAndDeletedFalse(UUID clinicId, UUID recipientId, Pageable pageable);

    long countByClinicIdAndRecipientIdAndDeletedFalseAndReadAtIsNull(UUID clinicId, UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.clinicId = :clinicId AND n.recipientId = :userId AND n.readAt IS NULL AND n.deleted = false")
    void markAllAsRead(@Param("clinicId") UUID clinicId, @Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);

    boolean existsByReferenceIdAndReferenceTypeAndDeletedFalse(UUID referenceId, String referenceType);

    @Query("SELECT n FROM Notification n WHERE n.deleted = false " +
            "AND n.status = :status AND n.channel = :channel " +
            "AND n.scheduledAt <= :before ORDER BY n.scheduledAt ASC")
     List<Notification> findPendingSmsNotifications(
             @Param("status") NotificationStatus status,
             @Param("channel") NotificationChannel channel,
             @Param("before") OffsetDateTime before,
             Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.channel = :channel AND n.scheduledAt <= :before AND n.deleted = false")
    List<Notification> findPendingEmailNotifications(
            @Param("status") NotificationStatus status,
            @Param("channel") NotificationChannel channel,
            @Param("before") OffsetDateTime before,
            Pageable pageable
    );

    boolean existsByReferenceIdAndReferenceTypeAndChannelAndDeletedFalse(
            UUID referenceId, String referenceType, NotificationChannel channel);

}
