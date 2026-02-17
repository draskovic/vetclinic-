package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateClinicRequest;
import com.softart.vetclinic.dto.UpdateClinicRequest;
import com.softart.vetclinic.enums.SubscriptionPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClinicControllerTest extends IntegrationTestBase {

    // Note: ClinicController does NOT use X-Clinic-Id header.
    // But we still need auth token, and our performGet/Post/Put/Delete include the header.
    // That's fine — the header is simply ignored by ClinicController.

    @Test
    @DisplayName("POST /api/clinics - create success")
    void create_success() throws Exception {
        var request = new CreateClinicRequest("New Clinic", "12345678",
                "new@clinic.test", "+381333333333", "Main St 1", "Nis", "Serbia",
                null, SubscriptionPlan.BASIC, null, true, "{}");

        performPost("/api/clinics", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("New Clinic"))
                .andExpect(jsonPath("$.city").value("Nis"));
    }

    @Test
    @DisplayName("POST /api/clinics - blank name returns 400")
    void create_blankName() throws Exception {
        var request = new CreateClinicRequest("", null, null, null, null, null, null,
                null, null, null, null, null);

        performPost("/api/clinics", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/clinics - get all success")
    void getAll_success() throws Exception {
        // clinicAId and clinicBId already seeded
        performGet("/api/clinics", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/clinics/{id} - get by ID success")
    void getById_success() throws Exception {
        performGet("/api/clinics/" + clinicAId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clinicAId.toString()))
                .andExpect(jsonPath("$.name").value("Clinic A"));
    }

    @Test
    @DisplayName("GET /api/clinics/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/clinics/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/clinics/{id} - update success")
    void update_success() throws Exception {
        var updateRequest = new UpdateClinicRequest("Clinic A Updated", null, null, null,
                null, null, null, null, null, null, null, null);

        performPut("/api/clinics/" + clinicAId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Clinic A Updated"));
    }

    @Test
    @DisplayName("DELETE /api/clinics/{id} - soft delete success")
    void delete_success() throws Exception {
        // Create a clinic to delete (don't delete the ones we need for auth)
        var request = new CreateClinicRequest("Temp Clinic", null, null, null, null, null, null,
                null, SubscriptionPlan.BASIC, null, true, "{}");

        String response = performPost("/api/clinics", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tempId = objectMapper.readTree(response).get("id").asText();

        performDelete("/api/clinics/" + tempId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }
}
