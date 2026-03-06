package com.softart.vetclinic.scheduler;

import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.enums.NotificationChannel;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.enums.RecipientType;
import com.softart.vetclinic.repository.NotificationRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.service.email.EmailDeliveryException;
import com.softart.vetclinic.service.email.EmailService;
import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSenderScheduler {

    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final OwnerRepository ownerRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void processPendingEmailNotifications() {
        List<Notification> pendingEmails = notificationRepository.findPendingEmailNotifications(
                NotificationStatus.PENDING,
                NotificationChannel.EMAIL,
                OffsetDateTime.now(),
                PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("Processing {} pending email notifications", pendingEmails.size());

        int sent = 0;
        int failed = 0;

        for (Notification notification : pendingEmails) {
            try {
                ClinicContextHolder.set(notification.getClinicId());

                String recipientEmail = resolveEmail(notification);
                if (recipientEmail == null) {
                    markAsFailed(notification, "Could not resolve email for recipient");
                    failed++;
                    continue;
                }

                String messageId = emailService.sendEmail(
                        recipientEmail,
                        notification.getTitle(),
                        notification.getMessage()
                );

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(OffsetDateTime.now());
                notification.setFailureReason(null);
                notificationRepository.save(notification);
                sent++;

            } catch (EmailDeliveryException e) {
                markAsFailed(notification, e.getMessage());
                failed++;
            } catch (Exception e) {
                markAsFailed(notification, "Unexpected error: " + e.getMessage());
                failed++;
            } finally {
                ClinicContextHolder.clear();
            }
        }

        log.info("Email processing complete: sent={}, failed={}", sent, failed);
    }

    private String resolveEmail(Notification notification) {
        if (notification.getRecipientType() == RecipientType.OWNER) {
            Optional<Owner> owner = ownerRepository.findByIdAndClinicIdAndDeletedFalse(
                    notification.getRecipientId(),
                    notification.getClinicId()
            );
            if (owner.isEmpty()) {
                log.warn("Owner not found for notification {}", notification.getId());
                return null;
            }
            String email = owner.get().getEmail();
            if (email == null || email.isBlank()) {
                log.warn("Owner {} has no email address", notification.getRecipientId());
                return null;
            }
            return email;
        }
        log.warn("Unsupported recipient type for email: {}", notification.getRecipientType());
        return null;
    }

    private void markAsFailed(Notification notification, String reason) {
        log.error("Email notification {} failed: {}", notification.getId(), reason);
        notification.setStatus(NotificationStatus.FAILED);
        String truncatedReason = reason != null && reason.length() > 1000
                ? reason.substring(0, 1000) : reason;
        notification.setFailureReason(truncatedReason);
        notificationRepository.save(notification);
    }
}
