package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateUserLocationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserLocationControllerTest extends IntegrationTestBase {

    private UUID locationId;

    private void seedPrerequisites() {
        locationId = seedClinicLocation(clinicAId, "Main Office", true);
    }

    @Test
    @DisplayName("POST /api/user-locations - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateUserLocationRequest(userAId, locationId, true);

        performPost("/api/user-locations", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/user-locations - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateUserLocationRequest(null, null, null);

        performPost("/api/user-locations", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/user-locations - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedUserLocation(clinicAId, userAId, locationId);

        performGet("/api/user-locations", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/user-locations/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/user-locations/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/user-locations/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID ulId = seedUserLocation(clinicAId, userAId, locationId);

        performDelete("/api/user-locations/" + ulId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/user-locations/by-user/{userId} - filter by user")
    void getByUser_success() throws Exception {
        seedPrerequisites();
        UUID location2Id = seedClinicLocation(clinicAId, "Branch", true);
        UUID user2Id = seedUser(clinicAId, roleAId, "vet@clinica.test", "Vet", "User");
        seedUserLocation(clinicAId, userAId, locationId);
        seedUserLocation(clinicAId, userAId, location2Id);
        seedUserLocation(clinicAId, user2Id, locationId);

        performGet("/api/user-locations/by-user/" + userAId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/user-locations - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedUserLocation(clinicAId, userAId, locationId);

        performGet("/api/user-locations", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedUserLocation(UUID clinicId, UUID userId, UUID locationId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO user_location (id, clinic_id, user_id, location_id, is_primary, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, true, NOW(), NOW(), false, 0)", id, clinicId, userId, locationId);
        return id;
    }
}
