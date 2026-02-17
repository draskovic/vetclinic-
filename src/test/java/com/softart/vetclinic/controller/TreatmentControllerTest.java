package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateTreatmentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TreatmentControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID petId;
    private UUID recordId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Marko", "Markovic", "+381611111111");
        petId = seedPet(clinicAId, ownerId, "Rex");
        recordId = seedMedicalRecord(clinicAId, petId, userAId);
    }

    @Test
    @DisplayName("POST /api/treatments - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateTreatmentRequest(recordId, null, userAId,
                "Wound cleaning", "Cleaned and disinfected wound", null, "Successful");

        performPost("/api/treatments", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Wound cleaning"));
    }

    @Test
    @DisplayName("POST /api/treatments - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateTreatmentRequest(null, null, null, "", null, null, null);

        performPost("/api/treatments", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/treatments - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedTreatment(clinicAId, recordId, userAId, "Treatment 1");

        performGet("/api/treatments", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/treatments/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/treatments/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/treatments/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID treatmentId = seedTreatment(clinicAId, recordId, userAId, "Temp Treatment");

        performDelete("/api/treatments/" + treatmentId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/treatments/" + treatmentId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/treatments/by-medical-record/{medicalRecordId} - filter by medical record")
    void getByMedicalRecord_success() throws Exception {
        seedPrerequisites();
        UUID record2Id = seedMedicalRecord(clinicAId, petId, userAId);
        seedTreatment(clinicAId, recordId, userAId, "Treatment 1");
        seedTreatment(clinicAId, recordId, userAId, "Treatment 2");
        seedTreatment(clinicAId, record2Id, userAId, "Treatment 3");

        performGet("/api/treatments/by-medical-record/" + recordId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/treatments - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedTreatment(clinicAId, recordId, userAId, "Treatment A");

        performGet("/api/treatments", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedTreatment(UUID clinicId, UUID medicalRecordId, UUID vetId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO treatment (id, clinic_id, medical_record_id, vet_id, name, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), false, 0)", id, clinicId, medicalRecordId, vetId, name);
        return id;
    }
}
