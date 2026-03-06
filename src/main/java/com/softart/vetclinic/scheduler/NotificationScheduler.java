package com.softart.vetclinic.scheduler;

import com.softart.vetclinic.entity.Appointment;
import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.entity.Vaccination;
import com.softart.vetclinic.enums.*;
import com.softart.vetclinic.repository.AppointmentRepository;
import com.softart.vetclinic.repository.NotificationRepository;
import com.softart.vetclinic.repository.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final AppointmentRepository appointmentRepository;
    private final VaccinationRepository vaccinationRepository;
    private final NotificationRepository notificationRepository;

    // Svaki dan u 8:00
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void createAppointmentReminders() {
        log.info("=== Kreiranje podsetnika za sutrašnje termine ===");

        OffsetDateTime tomorrowStart = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime tomorrowEnd = tomorrowStart.plusDays(1);

        List<Appointment> appointments = appointmentRepository
                .findTomorrowAppointments(tomorrowStart, tomorrowEnd);

        int count = 0;
        for (Appointment apt : appointments) {
            Notification template = new Notification();
            template.setClinicId(apt.getClinicId());
            template.setRecipientType(RecipientType.OWNER);
            template.setRecipientId(apt.getOwnerId());
            template.setType(NotificationType.APPOINTMENT_REMINDER);
            template.setTitle("Podsetnik za termin");
            template.setMessage(String.format(
                    "Poštovani, podsetnik: termin za vašeg ljubimca je zakazan za sutra (%s). Očekujemo vas!",
                    apt.getStartTime().toLocalDate()));
            template.setReferenceType("APPOINTMENT");
            template.setReferenceId(apt.getId());

            createNotification(template, NotificationChannel.SMS);
            createNotification(template, NotificationChannel.EMAIL);
            count++;
        }

        log.info("Kreirano podsetnika za {} termina (SMS + EMAIL)", count);
    }


    // Svaki dan u 9:00
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void createVaccinationReminders() {
        log.info("=== Kreiranje podsetnika za vakcinacije ===");

        LocalDate reminderDate = LocalDate.now().plusDays(7);
        List<Vaccination> vaccinations = vaccinationRepository
                .findUpcomingDueVaccinations(reminderDate);

        int count = 0;
        for (Vaccination vac : vaccinations) {
            Notification template = new Notification();
            template.setClinicId(vac.getClinicId());
            template.setRecipientType(RecipientType.OWNER);
            template.setRecipientId(vac.getPet().getOwnerId());
            template.setType(NotificationType.VACCINATION_DUE);
            template.setTitle("Podsetnik za vakcinaciju");
            template.setMessage(String.format(
                    "Poštovani, vakcinacija '%s' za vašeg ljubimca %s ističe %s. Pozovite nas za zakazivanje.",
                    vac.getVaccineName(), vac.getPet().getName(), vac.getNextDueDate()));
            template.setReferenceType("VACCINATION");
            template.setReferenceId(vac.getId());

            createNotification(template, NotificationChannel.SMS);
            createNotification(template, NotificationChannel.EMAIL);
            count++;
        }

        log.info("Kreirano podsetnika za {} vakcinacija (SMS + EMAIL)", count);
    }

    
    private void createNotification(Notification source, NotificationChannel channel) {
        boolean exists = notificationRepository
                .existsByReferenceIdAndReferenceTypeAndChannelAndDeletedFalse(
                        source.getReferenceId(), source.getReferenceType(), channel);
        if (exists) return;

        Notification notif = new Notification();
        notif.setClinicId(source.getClinicId());
        notif.setRecipientType(source.getRecipientType());
        notif.setRecipientId(source.getRecipientId());
        notif.setType(source.getType());
        notif.setChannel(channel);
        notif.setTitle(source.getTitle());
        notif.setMessage(source.getMessage());
        notif.setScheduledAt(OffsetDateTime.now());
        notif.setStatus(NotificationStatus.PENDING);
        notif.setReferenceType(source.getReferenceType());
        notif.setReferenceId(source.getReferenceId());

        notificationRepository.save(notif);
    }

}
