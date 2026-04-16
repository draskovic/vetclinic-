package com.softart.vetclinic.controller;

import com.softart.vetclinic.config.security.JwtPrincipal;
import com.softart.vetclinic.dto.BookingSettingsResponse;
import com.softart.vetclinic.dto.UpdateBookingSettingsRequest;
import com.softart.vetclinic.service.BookingSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking-settings")
@RequiredArgsConstructor
public class BookingSettingsController {

    private final BookingSettingsService bookingSettingsService;

    @GetMapping
    public ResponseEntity<BookingSettingsResponse> getSettings(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(bookingSettingsService.getOrCreate(principal.clinicId()));
    }

    @PutMapping
    public ResponseEntity<BookingSettingsResponse> updateSettings(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody UpdateBookingSettingsRequest request) {
        return ResponseEntity.ok(bookingSettingsService.update(principal.clinicId(), request));
    }
}
