package com.softart.vetclinic.controller;

import com.softart.vetclinic.config.security.JwtPrincipal;
import com.softart.vetclinic.dto.CreateNotificationRequest;
import com.softart.vetclinic.dto.NotificationResponse;
import com.softart.vetclinic.dto.UpdateNotificationRequest;
import com.softart.vetclinic.mapper.NotificationMapper;
import com.softart.vetclinic.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

   
    @GetMapping
    public Page<NotificationResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return notificationService.findAll(clinicId, pageable).map(notificationMapper::toResponse);
    }

    @GetMapping("/{id}")
    public NotificationResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return notificationMapper.toResponse(notificationService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateNotificationRequest request) {
        var entity = notificationMapper.toEntity(request);
        return notificationMapper.toResponse(notificationService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public NotificationResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNotificationRequest request) {
        return notificationMapper.toResponse(
                notificationService.update(id, clinicId, existing -> notificationMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        notificationService.softDelete(id, clinicId);
    }

    @GetMapping("/pending")
    public List<NotificationResponse> getPending(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before) {
        return notificationService.findPendingBefore(clinicId, before).stream()
                .map(notificationMapper::toResponse).toList();
    }
    
 // Notifikacije za ulogovanog korisnika
    @GetMapping("/my")
    public Page<NotificationResponse> getMyNotifications(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Authentication authentication,
            Pageable pageable) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return notificationService.findByRecipient(clinicId, principal.userId(), pageable)
                .map(notificationMapper::toResponse);
    }

    // Broj nepročitanih
    @GetMapping("/my/unread-count")
    public Map<String, Long> getUnreadCount(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        long count = notificationService.countUnread(clinicId, principal.userId());
        return Map.of("count", count);
    }

    // Označi jednu kao pročitanu
    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return notificationMapper.toResponse(notificationService.markAsRead(id, clinicId));
    }

    // Označi sve kao pročitane
    @PatchMapping("/my/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        notificationService.markAllAsRead(clinicId, principal.userId());
    }


}
