package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateVaccinationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VaccinationControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID petId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Jovan", "Jovanovic", "+381615555555");
        petId = seedPet(clinicAId, ownerId, "Bella");
    }

    @Test
    @DisplayName("POST /api/vaccinations - create success")
    void create_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime administered = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        var request = new CreateVaccinationRequest(petId, null, userAId,
                "Rabies", "BATCH-001", "PharmaVet", administered,
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 1));

        performPost("/api/vaccinations", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.vaccineName").value("Rabies"));
    }

    @Test
    @DisplayName("POST /api/vaccinations - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateVaccinationRequest(null, null, null,
                "", null, null, null, null, null);

        performPost("/api/vaccinations", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/vaccinations - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedVaccination(clinicAId, petId, userAId, "Rabies",
                OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), LocalDate.of(2027, 1, 1));

        performGet("/api/vaccinations", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/vaccinations/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/vaccinations/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/vaccinations/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID vaccinationId = seedVaccination(clinicAId, petId, userAId, "Parvo",
                OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), LocalDate.of(2027, 1, 1));

        performDelete("/api/vaccinations/" + vaccinationId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/vaccinations/by-pet/{petId} - filter by pet")
    void getByPet_success() throws Exception {
        seedPrerequisites();
        UUID pet2Id = seedPet(clinicAId, ownerId, "Max");
        seedVaccination(clinicAId, petId, userAId, "Rabies",
                OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), LocalDate.of(2027, 1, 1));
        seedVaccination(clinicAId, pet2Id, userAId, "Parvo",
                OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), LocalDate.of(2027, 1, 1));

        performGet("/api/vaccinations/by-pet/" + petId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/vaccinations/due - get due vaccinations")
    void getDue_success() throws Exception {
        seedPrerequisites();
        seedVaccination(clinicAId, petId, userAId, "Rabies",
                OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), LocalDate.of(2026, 1, 15));
        seedVaccination(clinicAId, petId, userAId, "Parvo",
                OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC), LocalDate.of(2027, 6, 1));

        performGet("/api/vaccinations/due?before=2026-03-01", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/vaccinations - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedVaccination(clinicAId, petId, userAId, "Rabies",
                OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC), null);

        performGet("/api/vaccinations", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedVaccination(UUID clinicId, UUID petId, UUID vetId, String vaccineName,
                                  OffsetDateTime administeredAt, LocalDate nextDueDate) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vaccination (id, clinic_id, pet_id, vet_id, vaccine_name, administered_at, next_due_date, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), false, 0)",
                id, clinicId, petId, vetId, vaccineName, administeredAt, nextDueDate);
        return id;
    }
}
