package com.softart.vetclinic.scheduler;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.enums.NotificationChannel;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.enums.RecipientType;
import com.softart.vetclinic.repository.NotificationRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.service.sms.SmsDeliveryException;
import com.softart.vetclinic.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsSenderScheduler {

    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final OwnerRepository ownerRepository;
    private final SmsService smsService;
    private final com.softart.vetclinic.repository.ClinicRepository clinicRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Svakih 5 minuta pokusava da posalje PENDING SMS notifikacije
     * ciji scheduledAt je u proslosti (ili sada).
     *
     * Per-notification tx: jedna failed notifikacija NE rollback-uje ostale.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void processPendingSmsNotifications() {
        log.info("=== Obrada PENDING SMS notifikacija ===");

        // 1. Load batch (notification table je global, no clinic context needed)
        List<Notification> pendingNotifications = transactionTemplate.execute(status ->
                notificationRepository.findPendingSmsNotifications(
                        NotificationStatus.PENDING,
                        NotificationChannel.SMS,
                        OffsetDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE)));

        if (pendingNotifications == null || pendingNotifications.isEmpty()) {
            log.debug("Nema PENDING SMS notifikacija za slanje");
            return;
        }

        log.info("Pronadjeno {} PENDING SMS notifikacija", pendingNotifications.size());

        int sent = 0;
        int failed = 0;

        for (Notification notification : pendingNotifications) {
            // Postavi context PRE per-iteration tx
            ClinicContextHolder.set(notification.getClinicId());
            try {
                boolean ok = processOne(notification);
                if (ok) sent++;
                else failed++;
            } catch (Exception e) {
                log.error("Neocekivana greska za SMS notifikaciju {}: {}",
                        notification.getId(), e.getMessage(), e);
                markAsFailed(notification, "Neocekivana greska: " + e.getMessage());
                failed++;
            } finally {
                ClinicContextHolder.clear();
            }
        }

        log.info("=== Zavrsena obrada SMS: poslato={}, neuspesno={} ===", sent, failed);
    }

    /**
     * Vraca true ako je notifikacija uspesno poslata i status SENT commit-ovan.
     * Marking as FAILED se desava u svojoj tx (commit-uje se cak i ako outer logic pukne).
     */
    private boolean processOne(Notification notification) {
        String phoneNumber = resolvePhoneNumber(notification);
        if (phoneNumber == null) {
            markAsFailed(notification, "Broj telefona nije pronadjen za primaoca "
                    + notification.getRecipientType() + ":" + notification.getRecipientId());
            return false;
        }

        String countryCode = clinicRepository.findById(notification.getClinicId())
                .map(c -> c.getPhoneCountryCode())
                .orElse("+381");

        try {
            smsService.sendSms(phoneNumber, notification.getMessage(), countryCode);
        } catch (SmsDeliveryException e) {
            markAsFailed(notification, e.getMessage());
            return false;
        }

        // SMS sent — commit SENT status u svojoj tx
        transactionTemplate.executeWithoutResult(status -> {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            notification.setFailureReason(null);
            notificationRepository.save(notification);
        });
        log.info("SMS notifikacija {} uspesno poslata na {}", notification.getId(), phoneNumber);
        return true;
    }

    /**
     * Pronalazi broj telefona na osnovu tipa primaoca.
     * Trenutno podrzava samo OWNER.
     */
    private String resolvePhoneNumber(Notification notification) {
        if (notification.getRecipientType() == RecipientType.OWNER) {
            Optional<Owner> owner = ownerRepository.findByIdAndClinicIdAndDeletedFalse(
                    notification.getRecipientId(), notification.getClinicId());

            if (owner.isPresent()) {
                String phone = owner.get().getPhone();
                if (phone == null || phone.isBlank()) {
                    log.warn("Owner {} nema telefon (notifikacija {})",
                            notification.getRecipientId(), notification.getId());
                    return null;
                }
                return phone;
            }
            log.warn("Owner {} nije pronadjen (notifikacija {})",
                    notification.getRecipientId(), notification.getId());
            return null;
        }

        log.warn("Nepodrzani tip primaoca za SMS: {} (notifikacija {})",
                notification.getRecipientType(), notification.getId());
        return null;
    }

    /**
     * Oznacava notifikaciju kao FAILED u svojoj tx.
     * Garantovano commit-ovan cak i ako outer logic baca exception kasnije.
     */
    private void markAsFailed(Notification notification, String reason) {
        log.error("SMS notifikacija {} neuspesna: {}", notification.getId(), reason);
        String truncated = (reason != null && reason.length() > 1000)
                ? reason.substring(0, 1000) : reason;
        transactionTemplate.executeWithoutResult(status -> {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(truncated);
            notificationRepository.save(notification);
        });
    }
}