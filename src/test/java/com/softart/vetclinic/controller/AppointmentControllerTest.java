package com.softart.vetclinic.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateAppointmentRequest;
import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.enums.AppointmentType;

class AppointmentControllerTest extends IntegrationTestBase {

    private UUID locationId;
    private UUID ownerId;
    private UUID petId;

    private void seedPrerequisites() {
        locationId = seedClinicLocation(clinicAId, "Main Office", true);
        ownerId = seedOwner(clinicAId, "Petar", "Petrovic", "+381611111111");
        petId = seedPet(clinicAId, ownerId, "Rex");
    }

    @Test
    @DisplayName("POST /api/appointments - create success")
    void create_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime start = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(1);

        var request = new CreateAppointmentRequest(locationId, petId, ownerId, userAId,
                start, end, AppointmentStatus.SCHEDULED, AppointmentType.CHECKUP, "Annual checkup", null, null);

        performPost("/api/appointments", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.type").value("CHECKUP"));
    }

    @Test
    @DisplayName("POST /api/appointments - invalid pet returns 404")
    void create_invalidPet() throws Exception {
        seedPrerequisites();
        OffsetDateTime start = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);

        var request = new CreateAppointmentRequest(locationId, UUID.randomUUID(), ownerId, userAId,
                start, start.plusHours(1), AppointmentStatus.SCHEDULED, AppointmentType.CHECKUP, null, null, null);

        performPost("/api/appointments", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/appointments - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateAppointmentRequest(null, null, null, null,
                null, null, null, null, null, null, null);

        performPost("/api/appointments", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/appointments - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime start = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start, start.plusHours(1), "SCHEDULED", "CHECKUP");

        performGet("/api/appointments", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} - get by ID success")
    void getById_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime start = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        UUID appointmentId = seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start, start.plusHours(1), "SCHEDULED", "CHECKUP");

        performGet("/api/appointments/" + appointmentId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/appointments/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime start = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        UUID appointmentId = seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start, start.plusHours(1), "SCHEDULED", "CHECKUP");

        performDelete("/api/appointments/" + appointmentId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/appointments/" + appointmentId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/appointments/date-range - filter by date range")
    void getByDateRange_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime start1 = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime start2 = OffsetDateTime.of(2026, 3, 20, 14, 0, 0, 0, ZoneOffset.UTC);

        seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start1, start1.plusHours(1), "SCHEDULED", "CHECKUP");
        seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start2, start2.plusHours(1), "SCHEDULED", "VACCINATION");

        String from = "2026-03-14T00:00:00Z";
        String to = "2026-03-16T23:59:59Z";

        performGet("/api/appointments/date-range?from=" + from + "&to=" + to, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/appointments/by-vet/{vetId} - filter by vet and date range")
    void getByVet_success() throws Exception {
        seedPrerequisites();
        UUID vet2Id = seedUser(clinicAId, roleAId, "vet2@clinica.test", "Dragan", "Dragic");

        OffsetDateTime start1 = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime start2 = OffsetDateTime.of(2026, 3, 15, 14, 0, 0, 0, ZoneOffset.UTC);

        seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start1, start1.plusHours(1), "SCHEDULED", "CHECKUP");
        seedAppointment(clinicAId, locationId, petId, ownerId, vet2Id,
                start2, start2.plusHours(1), "SCHEDULED", "VACCINATION");

        String from = "2026-03-14T00:00:00Z";
        String to = "2026-03-16T23:59:59Z";

        performGet("/api/appointments/by-vet/" + userAId + "?from=" + from + "&to=" + to, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/appointments - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        OffsetDateTime start = OffsetDateTime.of(2026, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        seedAppointment(clinicAId, locationId, petId, ownerId, userAId,
                start, start.plusHours(1), "SCHEDULED", "CHECKUP");

        performGet("/api/appointments", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        performGet("/api/appointments", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
