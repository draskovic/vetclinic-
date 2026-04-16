package com.softart.vetclinic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softart.vetclinic.dto.BookingAvailableSlotResponse;
import com.softart.vetclinic.entity.BookingSettings;
import com.softart.vetclinic.entity.ClinicLocation;
import com.softart.vetclinic.entity.User;
import com.softart.vetclinic.enums.AppointmentType;
import com.softart.vetclinic.repository.AppointmentRepository;
import com.softart.vetclinic.repository.BookingSettingsRepository;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import com.softart.vetclinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotAvailabilityService {

    private final BookingSettingsRepository bookingSettingsRepository;
    private final ClinicLocationRepository clinicLocationRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    

    public List<BookingAvailableSlotResponse> getAvailableSlots(
            UUID clinicId, UUID locationId, LocalDate date, AppointmentType type, UUID preferredVetId) {

        // 1. Učitaj i validiraj BookingSettings
        BookingSettings settings = bookingSettingsRepository.findByClinicId(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Online zakazivanje nije dostupno"));
        
        ZoneId clinicZone = ZoneId.of(settings.getTimezone());


        if (!settings.getEnabled()) {
            throw new IllegalArgumentException("Online zakazivanje nije omogućeno");
        }

        // Validacija datuma
        LocalDate today = LocalDate.now(clinicZone);
        if (!date.isAfter(today)) {
            throw new IllegalArgumentException("Datum mora biti u budućnosti");
        }
        if (date.isAfter(today.plusDays(settings.getMaxAdvanceDays()))) {
            throw new IllegalArgumentException("Maksimalno " + settings.getMaxAdvanceDays() + " dana unapred");
        }

        // Validacija tipa termina
        List<String> allowedTypes = parseAllowedTypes(settings.getAllowedTypes());
        if (!allowedTypes.contains(type.name())) {
            throw new IllegalArgumentException("Tip termina nije dozvoljen za online zakazivanje");
        }

        // 2. Učitaj radno vreme lokacije
        ClinicLocation location = clinicLocationRepository
                .findByIdAndClinicIdAndDeletedFalse(locationId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Lokacija nije pronađena"));

        List<Map<String, String>> periods = parseDayPeriods(location.getWorkingHours(), date);
        if (periods.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Učitaj veterinare
        List<User> vets;
        if (preferredVetId != null) {
            User vet = userRepository.findByIdAndClinicIdAndDeletedFalse(preferredVetId, clinicId)
                    .orElseThrow(() -> new IllegalArgumentException("Veterinar nije pronađen"));
            vets = List.of(vet);
        } else {
            vets = userRepository.findByClinicIdAndDeletedFalseAndActiveTrue(clinicId);
        }

        if (vets.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Generiši slotove za sve periode
        int slotMinutes = settings.getSlotDurationMinutes();
        int bufferMinutes = settings.getBufferMinutes();
        List<BookingAvailableSlotResponse> availableSlots = new ArrayList<>();

        for (Map<String, String> period : periods) {
            LocalTime workStart = LocalTime.parse(period.get("open"));
            LocalTime workEnd = LocalTime.parse(period.get("close"));

            LocalTime current = workStart;
            while (current.plusMinutes(slotMinutes).compareTo(workEnd) <= 0) {
                OffsetDateTime slotStart = date.atTime(current).atZone(clinicZone).toOffsetDateTime();
                OffsetDateTime slotEnd = slotStart.plusMinutes(slotMinutes);
                OffsetDateTime checkEnd = slotEnd.plusMinutes(bufferMinutes);

                for (User vet : vets) {
                    boolean hasOverlap = appointmentRepository.hasOverlappingAppointment(
                            clinicId, vet.getId(), slotStart, checkEnd, null);

                    if (!hasOverlap) {
                        String vetName = vet.getFirstName() + " " + vet.getLastName();
                        availableSlots.add(new BookingAvailableSlotResponse(
                                slotStart, slotEnd, vet.getId(), vetName));
                        break;
                    }
                }

                current = current.plusMinutes(slotMinutes);
            }
        }

        return availableSlots;
    }

    private List<String> parseAllowedTypes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Greška pri parsiranju allowedTypes: {}", e.getMessage());
            return List.of("CHECKUP", "VACCINATION", "GROOMING");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseDayPeriods(String workingHoursJson, LocalDate date) {
        if (workingHoursJson == null || workingHoursJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> allHours = objectMapper.readValue(workingHoursJson,
                    new TypeReference<Map<String, Object>>() {});

            String dayName = date.getDayOfWeek().name().toLowerCase();
            Object dayValue = allHours.get(dayName);

            if (dayValue == null) {
                return Collections.emptyList();
            }
            // Novi format: niz perioda [{"open":"10:00","close":"14:00"}, ...]
            if (dayValue instanceof List) {
                List<Map<String, String>> periods = new ArrayList<>();
                for (Object item : (List<?>) dayValue) {
                    if (item instanceof Map) {
                        Map<String, String> period = (Map<String, String>) item;
                        if (period.get("open") != null && period.get("close") != null) {
                            periods.add(period);
                        }
                    }
                }
                return periods;
            }
            // Stari format: {"open":"08:00","close":"16:00"}
            if (dayValue instanceof Map) {
                Map<String, String> single = (Map<String, String>) dayValue;
                if (single.get("open") != null && single.get("close") != null) {
                    return List.of(single);
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Greška pri parsiranju workingHours: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
