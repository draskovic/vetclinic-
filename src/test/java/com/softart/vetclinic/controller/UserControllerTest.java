package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateUserRequest;
import com.softart.vetclinic.dto.UpdateUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/users - create success")
    void create_success() throws Exception {
        var request = new CreateUserRequest(roleAId, "Marko", "Markovic",
                "marko@clinica.test", "password123", "+381612345678", "VET-001", "Surgery", true);

        performPost("/api/users", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Marko"))
                .andExpect(jsonPath("$.lastName").value("Markovic"))
                .andExpect(jsonPath("$.email").value("marko@clinica.test"));
    }

    @Test
    @DisplayName("POST /api/users - duplicate email returns 409")
    void create_duplicateEmail() throws Exception {
        var request = new CreateUserRequest(roleAId, "Another", "Admin",
                "admin@clinica.test", "password123", null, null, null, null);

        performPost("/api/users", tokenA, clinicAId, request)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/users - invalid role returns 404")
    void create_invalidRole() throws Exception {
        var request = new CreateUserRequest(UUID.randomUUID(), "Test", "User",
                "test@clinica.test", "password123", null, null, null, null);

        performPost("/api/users", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users - blank firstName returns 400")
    void create_blankFirstName() throws Exception {
        var request = new CreateUserRequest(roleAId, "", "Test",
                "test@clinica.test", "password123", null, null, null, null);

        performPost("/api/users", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/users - get all success")
    void getAll_success() throws Exception {
        // userAId already seeded
        performGet("/api/users", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("admin@clinica.test"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - get by ID success")
    void getById_success() throws Exception {
        performGet("/api/users/" + userAId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userAId.toString()))
                .andExpect(jsonPath("$.firstName").value("Admin"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/users/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - update success")
    void update_success() throws Exception {
        UUID userId = seedUser(clinicAId, roleAId, "vet@clinica.test", "Jovan", "Jovanovic");

        var updateRequest = new UpdateUserRequest(null, "Jovan", "Updated",
                null, null, null, "Dermatology", null);

        performPut("/api/users/" + userId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID userId = seedUser(clinicAId, roleAId, "temp@clinica.test", "Temp", "User");

        performDelete("/api/users/" + userId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/users/" + userId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/users - tenant isolation")
    void tenantIsolation() throws Exception {
        performGet("/api/users", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("admin@clinica.test"));

        performGet("/api/users", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("admin@clinicb.test"));
    }
}
