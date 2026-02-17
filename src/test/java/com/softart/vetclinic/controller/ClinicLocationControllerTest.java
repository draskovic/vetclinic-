package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateClinicLocationRequest;
import com.softart.vetclinic.dto.UpdateClinicLocationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClinicLocationControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/clinic-locations - create success")
    void create_success() throws Exception {
        var request = new CreateClinicLocationRequest("Main Office", "123 Main St",
                "Beograd", "+381111111111", "office@test.com", true, true, null);

        performPost("/api/clinic-locations", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Main Office"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/clinic-locations - blank name returns 400")
    void create_blankName() throws Exception {
        var request = new CreateClinicLocationRequest("", null, null, null, null, null, null, null);

        performPost("/api/clinic-locations", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/clinic-locations - get all success")
    void getAll_success() throws Exception {
        seedClinicLocation(clinicAId, "Main Office", true);
        seedClinicLocation(clinicAId, "Branch Office", false);

        performGet("/api/clinic-locations", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/clinic-locations/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID locationId = seedClinicLocation(clinicAId, "Main Office", true);

        performGet("/api/clinic-locations/" + locationId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId.toString()))
                .andExpect(jsonPath("$.name").value("Main Office"));
    }

    @Test
    @DisplayName("GET /api/clinic-locations/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/clinic-locations/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/clinic-locations/{id} - update success")
    void update_success() throws Exception {
        UUID locationId = seedClinicLocation(clinicAId, "Office", true);

        var updateRequest = new UpdateClinicLocationRequest("Main Office Updated",
                null, null, null, null, null, null, null);

        performPut("/api/clinic-locations/" + locationId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Office Updated"));
    }

    @Test
    @DisplayName("DELETE /api/clinic-locations/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID locationId = seedClinicLocation(clinicAId, "Temp Office", true);

        performDelete("/api/clinic-locations/" + locationId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/clinic-locations/" + locationId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/clinic-locations/active - filter active locations")
    void getActive_success() throws Exception {
        seedClinicLocation(clinicAId, "Active Office", true);
        seedClinicLocation(clinicAId, "Inactive Office", false);

        performGet("/api/clinic-locations/active", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Active Office"));
    }

    @Test
    @DisplayName("GET /api/clinic-locations - tenant isolation")
    void tenantIsolation() throws Exception {
        seedClinicLocation(clinicAId, "Location A", true);
        seedClinicLocation(clinicBId, "Location B", true);

        performGet("/api/clinic-locations", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Location A"));
    }
}
