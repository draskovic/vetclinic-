package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.NotificationChannel;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.enums.NotificationType;
import com.softart.vetclinic.enums.RecipientType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateNotificationRequest(
        RecipientType recipientType,
        UUID recipientId,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String message,
        OffsetDateTime scheduledAt,
        OffsetDateTime sentAt,
        NotificationStatus status,
        String referenceType,
        UUID referenceId
) {}
