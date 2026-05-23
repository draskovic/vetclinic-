package com.softart.vetclinic.scheduler;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.enums.NotificationChannel;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.enums.RecipientType;
import com.softart.vetclinic.repository.NotificationRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.service.email.EmailDeliveryException;
import com.softart.vetclinic.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    /**
     * Svakih 5 minuta pokusava da posalje PENDING email notifikacije
     * ciji scheduledAt je u proslosti (ili sada).
     *
     * Per-notification tx: jedna failed notifikacija NE rollback-uje ostale.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void processPendingEmailNotifications() {
        // 1. Load batch (notification table je global, no clinic context needed)
        List<Notification> pendingEmails = transactionTemplate.execute(status ->
                notificationRepository.findPendingEmailNotifications(
                        NotificationStatus.PENDING,
                        NotificationChannel.EMAIL,
                        OffsetDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE)));

        if (pendingEmails == null || pendingEmails.isEmpty()) {
            return;
        }

        log.info("Processing {} pending email notifications", pendingEmails.size());

        int sent = 0;
        int failed = 0;

        for (Notification notification : pendingEmails) {
            // Postavi context PRE per-iteration tx
            ClinicContextHolder.set(notification.getClinicId());
            try {
                boolean ok = processOne(notification);
                if (ok) sent++;
                else failed++;
            } catch (Exception e) {
                log.error("Unexpected error for email notification {}: {}",
                        notification.getId(), e.getMessage(), e);
                markAsFailed(notification, "Unexpected error: " + e.getMessage());
                failed++;
            } finally {
                ClinicContextHolder.clear();
            }
        }

        log.info("Email processing complete: sent={}, failed={}", sent, failed);
    }

    /**
     * Vraca true ako je email uspesno poslat i status SENT commit-ovan.
     * markAsFailed se desava u svojoj tx (commit-uje se cak i ako outer logic pukne).
     */
    private boolean processOne(Notification notification) {
        String recipientEmail = resolveEmail(notification);
        if (recipientEmail == null) {
            markAsFailed(notification, "Could not resolve email for recipient");
            return false;
        }

        try {
            emailService.sendEmail(
                    recipientEmail,
                    notification.getTitle(),
                    notification.getMessage()
            );
        } catch (EmailDeliveryException e) {
            markAsFailed(notification, e.getMessage());
            return false;
        }

        // Email sent — commit SENT status u svojoj tx
        transactionTemplate.executeWithoutResult(status -> {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            notification.setFailureReason(null);
            notificationRepository.save(notification);
        });
        return true;
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

    /**
     * Oznacava notifikaciju kao FAILED u svojoj tx.
     * Garantovano commit-ovan cak i ako outer logic baca exception kasnije.
     */
    private void markAsFailed(Notification notification, String reason) {
        log.error("Email notification {} failed: {}", notification.getId(), reason);
        String truncatedReason = (reason != null && reason.length() > 1000)
                ? reason.substring(0, 1000) : reason;
        transactionTemplate.executeWithoutResult(status -> {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(truncatedReason);
            notificationRepository.save(notification);
        });
    }
}