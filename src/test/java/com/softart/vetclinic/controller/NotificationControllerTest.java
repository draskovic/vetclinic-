package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateNotificationRequest;
import com.softart.vetclinic.enums.NotificationChannel;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.enums.NotificationType;
import com.softart.vetclinic.enums.RecipientType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest extends IntegrationTestBase {

    private UUID ownerId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Zoran", "Zoranic", "+381610000000");
    }

    @Test
    @DisplayName("POST /api/notifications - create success")
    void create_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime scheduled = OffsetDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        var request = new CreateNotificationRequest(RecipientType.OWNER, ownerId,
                NotificationType.APPOINTMENT_REMINDER, NotificationChannel.EMAIL,
                "Reminder", "Your appointment is tomorrow",
                scheduled, null, NotificationStatus.PENDING, null, null);

        performPost("/api/notifications", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Reminder"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/notifications - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateNotificationRequest(null, null, null, null,
                "", "", null, null, null, null, null);

        performPost("/api/notifications", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/notifications - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedNotification(clinicAId, ownerId, "PENDING",
                OffsetDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC));

        performGet("/api/notifications", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/notifications/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/notifications/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID notifId = seedNotification(clinicAId, ownerId, "PENDING",
                OffsetDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC));

        performDelete("/api/notifications/" + notifId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/notifications/pending - get pending before date")
    void getPending_success() throws Exception {
        seedPrerequisites();
        seedNotification(clinicAId, ownerId, "PENDING",
                OffsetDateTime.of(2026, 2, 15, 9, 0, 0, 0, ZoneOffset.UTC));
        seedNotification(clinicAId, ownerId, "PENDING",
                OffsetDateTime.of(2026, 4, 1, 9, 0, 0, 0, ZoneOffset.UTC));
        seedNotification(clinicAId, ownerId, "SENT",
                OffsetDateTime.of(2026, 2, 10, 9, 0, 0, 0, ZoneOffset.UTC));

        performGet("/api/notifications/pending?before=2026-03-01T00:00:00Z", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/notifications - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedNotification(clinicAId, ownerId, "PENDING",
                OffsetDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC));

        performGet("/api/notifications", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedNotification(UUID clinicId, UUID recipientId, String status, OffsetDateTime scheduledAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO notification (id, clinic_id, recipient_type, recipient_id, type, channel, title, message, scheduled_at, status, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, 'OWNER', ?, 'APPOINTMENT_REMINDER', 'EMAIL', 'Reminder', 'Your appointment is coming up', ?, ?, NOW(), NOW(), false, 0)",
                id, clinicId, recipientId, scheduledAt, status);
        return id;
    }
}
