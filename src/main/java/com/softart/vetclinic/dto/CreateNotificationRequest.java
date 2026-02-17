package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.NotificationChannel;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.enums.NotificationType;
import com.softart.vetclinic.enums.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull RecipientType recipientType,
        @NotNull UUID recipientId,
        @NotNull NotificationType type,
        @NotNull NotificationChannel channel,
        @NotBlank String title,
        @NotBlank String message,
        OffsetDateTime scheduledAt,
        OffsetDateTime sentAt,
        NotificationStatus status,
        String referenceType,
        UUID referenceId
) {}
