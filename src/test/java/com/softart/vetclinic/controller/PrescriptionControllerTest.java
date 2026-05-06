package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreatePrescriptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PrescriptionControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID petId;
    private UUID recordId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Stefan", "Stefanovic", "+381616666666");
        petId = seedPet(clinicAId, ownerId, "Lucky");
        recordId = seedMedicalRecord(clinicAId, petId, userAId);
    }

    @Test
    @DisplayName("POST /api/prescriptions - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreatePrescriptionRequest(recordId, petId, userAId,
                "Amoxicillin", "500mg", "Twice daily", 7,
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 17), "Take with food", null);

        performPost("/api/prescriptions", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.medicationName").value("Amoxicillin"));
    }

    @Test
    @DisplayName("POST /api/prescriptions - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreatePrescriptionRequest(null, null, null,
                "", "", "", null, null, null, null, null);

        performPost("/api/prescriptions", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/prescriptions - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedPrescription(clinicAId, recordId, petId, userAId, "Amoxicillin");

        performGet("/api/prescriptions", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/prescriptions/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/prescriptions/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/prescriptions/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID prescriptionId = seedPrescription(clinicAId, recordId, petId, userAId, "Temp Med");

        performDelete("/api/prescriptions/" + prescriptionId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/prescriptions/by-medical-record/{medicalRecordId} - filter by medical record")
    void getByMedicalRecord_success() throws Exception {
        seedPrerequisites();
        UUID record2Id = seedMedicalRecord(clinicAId, petId, userAId);
        seedPrescription(clinicAId, recordId, petId, userAId, "Med 1");
        seedPrescription(clinicAId, recordId, petId, userAId, "Med 2");
        seedPrescription(clinicAId, record2Id, petId, userAId, "Med 3");

        performGet("/api/prescriptions/by-medical-record/" + recordId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/prescriptions/by-pet/{petId} - filter by pet")
    void getByPet_success() throws Exception {
        seedPrerequisites();
        seedPrescription(clinicAId, recordId, petId, userAId, "Med 1");

        performGet("/api/prescriptions/by-pet/" + petId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/prescriptions - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedPrescription(clinicAId, recordId, petId, userAId, "Med A");

        performGet("/api/prescriptions", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedPrescription(UUID clinicId, UUID medicalRecordId, UUID petId, UUID vetId, String medicationName) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO prescription (id, clinic_id, medical_record_id, pet_id, vet_id, medication_name, dosage, frequency, start_date, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, '100mg', 'Daily', '2026-02-10', NOW(), NOW(), false, 0)",
                id, clinicId, medicalRecordId, petId, vetId, medicationName);
        return id;
    }
}
