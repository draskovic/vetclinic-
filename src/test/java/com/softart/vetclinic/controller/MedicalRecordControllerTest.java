package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateMedicalRecordRequest;
import com.softart.vetclinic.dto.UpdateMedicalRecordRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MedicalRecordControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID petId;
    private UUID locationId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Ana", "Anic", "+381612222222");
        petId = seedPet(clinicAId, ownerId, "Miki");
        locationId = seedClinicLocation(clinicAId, "Main", true);
    }

    @Test
    @DisplayName("POST /api/medical-records - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateMedicalRecordRequest(null, petId, userAId,
                "Vomiting", "Gastritis", "Physical exam normal", new BigDecimal("5.2"),
                new BigDecimal("38.5"), 80, false, null);

        performPost("/api/medical-records", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.diagnosis").value("Gastritis"));
    }

    @Test
    @DisplayName("POST /api/medical-records - invalid pet returns 404")
    void create_invalidPet() throws Exception {
        var request = new CreateMedicalRecordRequest(null, UUID.randomUUID(), userAId,
                null, null, null, null, null, null, null, null);

        performPost("/api/medical-records", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/medical-records - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateMedicalRecordRequest(null, null, null,
                null, null, null, null, null, null, null, null);

        performPost("/api/medical-records", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/medical-records - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedMedicalRecord(clinicAId, petId, userAId);

        performGet("/api/medical-records", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/medical-records/{id} - get by ID success")
    void getById_success() throws Exception {
        seedPrerequisites();
        UUID recordId = seedMedicalRecord(clinicAId, petId, userAId);

        performGet("/api/medical-records/" + recordId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId.toString()));
    }

    @Test
    @DisplayName("GET /api/medical-records/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/medical-records/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/medical-records/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID recordId = seedMedicalRecord(clinicAId, petId, userAId);

        performDelete("/api/medical-records/" + recordId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/medical-records/" + recordId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/medical-records/by-pet/{petId} - filter by pet")
    void getByPet_success() throws Exception {
        seedPrerequisites();
        UUID pet2Id = seedPet(clinicAId, ownerId, "Luna");
        seedMedicalRecord(clinicAId, petId, userAId);
        seedMedicalRecord(clinicAId, petId, userAId);
        seedMedicalRecord(clinicAId, pet2Id, userAId);

        performGet("/api/medical-records/by-pet/" + petId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/medical-records/by-appointment/{appointmentId} - find by appointment")
    void getByAppointment_notFound() throws Exception {
        performGet("/api/medical-records/by-appointment/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/medical-records - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedMedicalRecord(clinicAId, petId, userAId);

        performGet("/api/medical-records", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        performGet("/api/medical-records", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
