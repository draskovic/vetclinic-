package com.softart.vetclinic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softart.vetclinic.config.BookingRateLimiter;
import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.dto.*;
import com.softart.vetclinic.entity.*;
import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.enums.BookingSource;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicBookingService {

    private final ClinicRepository clinicRepository;
    private final BookingSettingsRepository bookingSettingsRepository;
    private final ClinicLocationRepository clinicLocationRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final SpeciesRepository speciesRepository;
    private final AppointmentRepository appointmentRepository;
    private final OwnerService ownerService;
    private final PetService petService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final BookingRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public BookingClinicInfoResponse getClinicInfo(UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Klinika nije pronađena"));

        BookingSettings settings = bookingSettingsRepository.findByClinicId(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Online zakazivanje nije dostupno"));

        if (!settings.getEnabled()) {
            throw new IllegalArgumentException("Online zakazivanje nije omogućeno za ovu kliniku");
        }

        // Učitaj lokacije — moramo setovati RLS kontekst
        List<BookingClinicInfoResponse.LocationInfo> locations;
        List<BookingClinicInfoResponse.VetInfo> vets = null;
        try {
            ClinicContextHolder.set(clinicId);

            locations = clinicLocationRepository.findByClinicIdAndDeletedFalseAndActiveTrue(clinicId).stream()
                    .filter(loc -> loc.getWorkingHours() != null)
                    .map(loc -> new BookingClinicInfoResponse.LocationInfo(
                            loc.getId(), loc.getName(), loc.getAddress()))
                    .collect(Collectors.toList());


            if (settings.getAllowVetSelection()) {
                vets = userRepository.findByClinicIdAndDeletedFalseAndActiveTrue(clinicId).stream()
                        .map(u -> new BookingClinicInfoResponse.VetInfo(
                                u.getId(),
                                u.getFirstName() + " " + u.getLastName(),
                                u.getSpecialization()))
                        .collect(Collectors.toList());
            }
        } finally {
            ClinicContextHolder.clear();
        }

        List<String> allowedTypes = parseAllowedTypes(settings.getAllowedTypes());

        return new BookingClinicInfoResponse(
                clinic.getName(),
                clinic.getPhone(),
                clinic.getAddress(),
                clinic.getCity(),
                clinic.getLogoUrl() != null ? "/public/booking/" + clinicId + "/logo" : null,
                locations,
                allowedTypes,
                settings.getSlotDurationMinutes(),
                settings.getMaxAdvanceDays(),
                settings.getAllowVetSelection(),
                vets
        );
    }

    public BookingOwnerLookupResponse lookupOwner(UUID clinicId, String phone) {
        try {
            ClinicContextHolder.set(clinicId);

            List<Owner> owners = ownerRepository.findByClinicIdAndDeletedFalseAndPhone(clinicId, phone);
            if (owners.isEmpty()) {
                return new BookingOwnerLookupResponse(false, null, null, null);
            }

            Owner owner = owners.get(0); // Uzmi prvog ako ih ima više
            List<Pet> pets = petRepository.findByClinicIdAndOwnerIdAndDeletedFalse(clinicId, owner.getId());

            List<BookingOwnerLookupResponse.PetInfo> petInfos = pets.stream()
                    .map(pet -> {
                        String speciesName = null;
                        if (pet.getSpeciesId() != null) {
                            speciesName = speciesRepository.findById(pet.getSpeciesId())
                                    .map(Species::getName)
                                    .orElse(null);
                        }
                        return new BookingOwnerLookupResponse.PetInfo(
                                pet.getId(), pet.getName(), speciesName);
                    })
                    .collect(Collectors.toList());

            return new BookingOwnerLookupResponse(
                    true,
                    owner.getId(),
                    owner.getFirstName() + " " + owner.getLastName(),
                    petInfos
            );
        } finally {
            ClinicContextHolder.clear();
        }
    }

    @Transactional
    public BookingCreateResponse createBooking(UUID clinicId, BookingCreateRequest request, String clientIp) {
        // 1. Honeypot — tiho "uspeh" za botove
        if (request.honeypot() != null && !request.honeypot().isBlank()) {
            log.warn("Honeypot triggered za clinicId={}, phone={}", clinicId, request.phone());
            return new BookingCreateResponse(UUID.randomUUID(), AppointmentStatus.PENDING,
                    "Vaš termin je uspešno zakazan");
        }

        // 2. Rate limit
        rateLimiter.checkLimit("phone:" + request.phone());
        if (clientIp != null) {
            rateLimiter.checkLimit("ip:" + clientIp);
        }

        // 3. Proveri settings (context već postavljen iz kontrolera)
        BookingSettings settings = bookingSettingsRepository.findByClinicId(clinicId)
                .orElseThrow(() -> new BadRequestException("Online zakazivanje nije dostupno"));
        if (!settings.getEnabled()) {
            throw new BadRequestException("Online zakazivanje nije omogućeno");
        }

        // 4. Owner resolucija
        UUID ownerId = resolveOwner(clinicId, request);

        // 5. Pet resolucija
        UUID petId = resolvePet(clinicId, ownerId, request);

        // 6. Vet resolucija — proveri da je slot još slobodan
        ZoneId clinicZone = ZoneId.of(settings.getTimezone());
        var slots = slotAvailabilityService.getAvailableSlots(
                clinicId, request.locationId(),
                request.startTime().atZoneSameInstant(clinicZone).toLocalDate(),
                request.type(), request.preferredVetId());

        UUID vetId = slots.stream()
                .filter(s -> s.startTime().isEqual(request.startTime()))
                .map(BookingAvailableSlotResponse::vetId)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Izabrani termin više nije dostupan. Molimo izaberite drugi."));

        // 7. Kreiraj appointment
        AppointmentStatus status = settings.getAutoConfirm()
                ? AppointmentStatus.CONFIRMED
                : AppointmentStatus.PENDING;

        Appointment appointment = new Appointment();
        appointment.setClinicId(clinicId);
        appointment.setLocationId(request.locationId());
        appointment.setPetId(petId);
        appointment.setOwnerId(ownerId);
        appointment.setVetId(vetId);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.startTime().plusMinutes(settings.getSlotDurationMinutes()));
        appointment.setStatus(status);
        appointment.setType(request.type());
        appointment.setReason(request.reason());
        appointment.setBookingSource(BookingSource.ONLINE);
        appointment.setCancellationToken(UUID.randomUUID().toString());

        Appointment saved = appointmentRepository.save(appointment);

        String message = settings.getAutoConfirm()
                ? "Vaš termin je uspešno zakazan!"
                : "Vaš zahtev je primljen. Klinika će potvrditi termin u najkraćem roku.";

        log.info("Online booking kreiran: id={}, clinicId={}, status={}", saved.getId(), clinicId, status);

        return new BookingCreateResponse(saved.getId(), status, message);
    }

    public void cancelBooking(String cancellationToken) {
        Appointment appointment = appointmentRepository
                .findByCancellationTokenAndDeletedFalse(cancellationToken)
                .orElseThrow(() -> new IllegalArgumentException("Nevažeći token za otkazivanje"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Termin je već otkazan");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Završen termin se ne može otkazati");
        }

        // Proveri cancellation deadline
        BookingSettings settings = bookingSettingsRepository.findByClinicId(appointment.getClinicId())
                .orElse(null);
        if (settings != null && settings.getCancellationHours() > 0) {
            ZoneId clinicZone = ZoneId.of(settings.getTimezone());
            var now = java.time.OffsetDateTime.now(clinicZone);
            var deadline = appointment.getStartTime().minusHours(settings.getCancellationHours());
            if (now.isAfter(deadline)) {
                throw new BadRequestException("Otkazivanje nije moguće manje od "
                        + settings.getCancellationHours() + "h pre termina");
            }
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        log.info("Online booking otkazan: id={}, token={}", appointment.getId(), cancellationToken);
    }

    private UUID resolveOwner(UUID clinicId, BookingCreateRequest request) {
        List<Owner> existing = ownerRepository.findByClinicIdAndDeletedFalseAndPhone(clinicId, request.phone());
        if (!existing.isEmpty()) {
            return existing.get(0).getId();
        }

        // Kreiraj novog vlasnika
        if (request.firstName() == null || request.firstName().isBlank()
                || request.lastName() == null || request.lastName().isBlank()) {
            throw new BadRequestException("Ime i prezime su obavezni za nove klijente");
        }

        Owner owner = new Owner();
        owner.setClinicId(clinicId);
        owner.setFirstName(request.firstName().trim());
        owner.setLastName(request.lastName().trim());
        owner.setPhone(request.phone().trim());
        owner.setEmail(request.email() != null ? request.email().trim() : null);

        Owner saved = ownerService.createWithClientCode(owner, clinicId);
        log.info("Novi vlasnik kreiran putem online booking-a: id={}, phone={}", saved.getId(), saved.getPhone());
        return saved.getId();
    }

    private UUID resolvePet(UUID clinicId, UUID ownerId, BookingCreateRequest request) {
        // Ako je izabran postojeći ljubimac
        if (request.petId() != null) {
            boolean exists = petRepository.existsByIdAndClinicIdAndDeletedFalse(request.petId(), clinicId);
            if (!exists) {
                throw new BadRequestException("Ljubimac nije pronađen");
            }
            return request.petId();
        }

        // Kreiraj novog ljubimca
        if (request.petName() == null || request.petName().isBlank()) {
            throw new BadRequestException("Ime ljubimca je obavezno");
        }

        Pet pet = new Pet();
        pet.setClinicId(clinicId);
        pet.setOwnerId(ownerId);
        pet.setName(request.petName().trim());

        // Resolucija vrste po imenu
        if (request.speciesName() != null && !request.speciesName().isBlank()) {
            speciesRepository.findByClinicIdAndNameIgnoreCaseAndDeletedFalse(clinicId, request.speciesName().trim())
                    .ifPresent(species -> pet.setSpeciesId(species.getId()));
        }

        Pet saved = petService.createWithPatientCode(pet, clinicId);
        log.info("Novi ljubimac kreiran putem online booking-a: id={}, name={}", saved.getId(), saved.getName());
        return saved.getId();
    }

    private List<String> parseAllowedTypes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("CHECKUP", "VACCINATION", "GROOMING");
        }
    }
}
