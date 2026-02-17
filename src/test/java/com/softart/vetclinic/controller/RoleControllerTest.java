package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateRoleRequest;
import com.softart.vetclinic.dto.UpdateRoleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoleControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/roles - create success")
    void create_success() throws Exception {
        var request = new CreateRoleRequest("VET", "[\"appointments:*\",\"medical_records:*\"]");

        performPost("/api/roles", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("VET"));
    }

    @Test
    @DisplayName("POST /api/roles - duplicate name returns 409")
    void create_duplicateName() throws Exception {
        // ADMIN role already seeded in setUp
        var request = new CreateRoleRequest("ADMIN", "[\"*\"]");

        performPost("/api/roles", tokenA, clinicAId, request)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/roles - blank name returns 400")
    void create_blankName() throws Exception {
        var request = new CreateRoleRequest("", null);

        performPost("/api/roles", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/roles - get all success")
    void getAll_success() throws Exception {
        // ADMIN role already seeded, add another
        seedRole(clinicAId, "VET", "[\"appointments:*\"]");

        performGet("/api/roles", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/roles/{id} - get by ID success")
    void getById_success() throws Exception {
        performGet("/api/roles/" + roleAId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleAId.toString()))
                .andExpect(jsonPath("$.name").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/roles/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/roles/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/roles/{id} - update success")
    void update_success() throws Exception {
        UUID roleId = seedRole(clinicAId, "VET", "[\"appointments:*\"]");

        var updateRequest = new UpdateRoleRequest("VETERINARIAN", null);

        performPut("/api/roles/" + roleId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VETERINARIAN"));
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID roleId = seedRole(clinicAId, "RECEPTIONIST", "[\"owners:*\"]");

        performDelete("/api/roles/" + roleId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/roles/" + roleId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/roles - tenant isolation")
    void tenantIsolation() throws Exception {
        seedRole(clinicBId, "VET_B", "[\"*\"]");

        // Clinic A sees only ADMIN (seeded in setUp)
        performGet("/api/roles", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("ADMIN"));
    }
}
