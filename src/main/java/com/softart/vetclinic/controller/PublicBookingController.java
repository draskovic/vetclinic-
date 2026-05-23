package com.softart.vetclinic.controller;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.dto.*;
import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.enums.AppointmentType;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.service.PublicBookingService;
import com.softart.vetclinic.service.SlotAvailabilityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.softart.vetclinic.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/public/booking")
@RequiredArgsConstructor
public class PublicBookingController {

    private final PublicBookingService bookingService;
    private final SlotAvailabilityService slotAvailabilityService;
    private final ClinicRepository clinicRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/{clinicId}/info")
    public ResponseEntity<BookingClinicInfoResponse> getClinicInfo(@PathVariable UUID clinicId) {
        try {
            return ResponseEntity.ok(bookingService.getClinicInfo(clinicId));
        } catch (IllegalArgumentException e) {
            log.warn("Booking info nedostupan za clinicId={}: {}", clinicId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{clinicId}/slots")
    public ResponseEntity<List<BookingAvailableSlotResponse>> getAvailableSlots(
            @PathVariable UUID clinicId,
            @RequestParam UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam AppointmentType type,
            @RequestParam(required = false) UUID vetId) {
        try {
            List<BookingAvailableSlotResponse> slots = slotAvailabilityService
                    .getAvailableSlots(clinicId, locationId, date, type, vetId);
            return ResponseEntity.ok(slots);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{clinicId}/owner-lookup")
    public ResponseEntity<BookingOwnerLookupResponse> lookupOwner(
            @PathVariable UUID clinicId,
            @RequestParam String phone) {
        return ResponseEntity.ok(bookingService.lookupOwner(clinicId, phone));
    }

    @PostMapping("/{clinicId}/book")
    public ResponseEntity<BookingCreateResponse> createBooking(
            @PathVariable UUID clinicId,
            @RequestBody @Valid BookingCreateRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        ClinicContextHolder.set(clinicId);
        try {
            BookingCreateResponse response = bookingService.createBooking(clinicId, request, clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Booking greška za clinicId={}: {}", clinicId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    new BookingCreateResponse(null, null, e.getMessage()));
        } finally {
            ClinicContextHolder.clear();
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@RequestParam String token) {
        try {
            bookingService.cancelBooking(token);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Termin je uspešno otkazan"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    @GetMapping("/{clinicId}/logo")
    public ResponseEntity<Resource> getClinicLogo(@PathVariable UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Klinika nije pronađena"));
        
        if (clinic.getLogoUrl() == null || clinic.getLogoUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Resource resource = fileStorageService.load(clinic.getLogoUrl());
            String contentType = "image/png";
            String path = clinic.getLogoUrl().toLowerCase();
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (path.endsWith(".gif")) contentType = "image/gif";
            else if (path.endsWith(".webp")) contentType = "image/webp";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Cache-Control", "public, max-age=3600")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
