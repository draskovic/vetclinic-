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
            // Proveri da li notifikacija već postoji za ovaj termin
            boolean exists = notificationRepository
                    .existsByReferenceIdAndReferenceTypeAndDeletedFalse(
                            apt.getId(), "APPOINTMENT");
            if (exists) continue;

            Notification notif = new Notification();
            notif.setClinicId(apt.getClinicId());
            notif.setRecipientType(RecipientType.OWNER);
            notif.setRecipientId(apt.getOwnerId());
            notif.setType(NotificationType.APPOINTMENT_REMINDER);
            notif.setChannel(NotificationChannel.SMS);
            notif.setTitle("Podsetnik za termin");
            notif.setMessage(String.format("Poštovani, podsetnik: termin za vašeg ljubimca je zakazan za sutra (%s). Očekujemo vas!",
                    apt.getStartTime().toLocalDate()));
            notif.setScheduledAt(OffsetDateTime.now());
            notif.setStatus(NotificationStatus.PENDING);
            notif.setReferenceType("APPOINTMENT");
            notif.setReferenceId(apt.getId());

            notificationRepository.save(notif);
            count++;
        }
        log.info("Kreirano {} podsetnika za termine", count);
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
            // Proveri da li notifikacija već postoji za ovu vakcinaciju
            boolean exists = notificationRepository
                    .existsByReferenceIdAndReferenceTypeAndDeletedFalse(
                            vac.getId(), "VACCINATION");
            if (exists) continue;

            Notification notif = new Notification();
            notif.setClinicId(vac.getClinicId());
            notif.setRecipientType(RecipientType.OWNER);
            notif.setRecipientId(vac.getPet().getOwnerId());
            notif.setType(NotificationType.VACCINATION_DUE);
            notif.setChannel(NotificationChannel.SMS);
            notif.setTitle("Podsetnik za vakcinaciju");
            notif.setMessage(String.format("Poštovani, vakcinacija '%s' za vašeg ljubimca %s ističe %s. Pozovite nas za zakazivanje.",
                    vac.getVaccineName(), vac.getPet().getName(), vac.getNextDueDate()));
            notif.setScheduledAt(OffsetDateTime.now());
            notif.setStatus(NotificationStatus.PENDING);
            notif.setReferenceType("VACCINATION");
            notif.setReferenceId(vac.getId());

            notificationRepository.save(notif);
            count++;
        }
        log.info("Kreirano {} podsetnika za vakcinacije", count);
    }
}
