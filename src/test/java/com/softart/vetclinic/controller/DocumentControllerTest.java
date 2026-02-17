package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateDocumentRequest;
import com.softart.vetclinic.enums.FileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID petId;
    private UUID recordId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Luka", "Lukic", "+381619999999");
        petId = seedPet(clinicAId, ownerId, "Lajka");
        recordId = seedMedicalRecord(clinicAId, petId, userAId);
    }

    @Test
    @DisplayName("POST /api/documents - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateDocumentRequest(petId, recordId, userAId,
                "xray-chest.jpg", FileType.XRAY, "image/jpeg",
                1024L, "/uploads/xray-chest.jpg", "Chest X-ray");

        performPost("/api/documents", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fileName").value("xray-chest.jpg"))
                .andExpect(jsonPath("$.fileType").value("XRAY"));
    }

    @Test
    @DisplayName("POST /api/documents - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateDocumentRequest(null, null, null,
                "", null, null, null, "", null);

        performPost("/api/documents", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/documents - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedDocument(clinicAId, petId, recordId, "doc1.pdf");

        performGet("/api/documents", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/documents/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/documents/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID docId = seedDocument(clinicAId, petId, recordId, "temp.pdf");

        performDelete("/api/documents/" + docId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/documents/by-pet/{petId} - filter by pet")
    void getByPet_success() throws Exception {
        seedPrerequisites();
        UUID pet2Id = seedPet(clinicAId, ownerId, "Snoopy");
        seedDocument(clinicAId, petId, null, "doc1.pdf");
        seedDocument(clinicAId, petId, null, "doc2.pdf");
        seedDocument(clinicAId, pet2Id, null, "doc3.pdf");

        performGet("/api/documents/by-pet/" + petId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/documents/by-medical-record/{medicalRecordId} - filter by medical record")
    void getByMedicalRecord_success() throws Exception {
        seedPrerequisites();
        UUID record2Id = seedMedicalRecord(clinicAId, petId, userAId);
        seedDocument(clinicAId, petId, recordId, "doc1.pdf");
        seedDocument(clinicAId, petId, record2Id, "doc2.pdf");

        performGet("/api/documents/by-medical-record/" + recordId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/documents - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedDocument(clinicAId, petId, null, "doc_a.pdf");

        performGet("/api/documents", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedDocument(UUID clinicId, UUID petId, UUID medicalRecordId, String fileName) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO document (id, clinic_id, pet_id, medical_record_id, uploaded_by, file_name, file_type, storage_path, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PDF', '/uploads/test', NOW(), NOW(), false, 0)",
                id, clinicId, petId, medicalRecordId, userAId, fileName);
        return id;
    }
}
