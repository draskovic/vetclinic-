package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateOwnerRequest;
import com.softart.vetclinic.dto.UpdateOwnerRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OwnerControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/owners - create success")
    void create_success() throws Exception {
        var request = new CreateOwnerRequest(
                "Marko", "Petrovic", "marko@test.com", "+381641234567",
                "Knez Mihailova 10", "Beograd", null, null, null);

        performPost("/api/owners", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Marko"))
                .andExpect(jsonPath("$.lastName").value("Petrovic"))
                .andExpect(jsonPath("$.phone").value("+381641234567"))
                .andExpect(jsonPath("$.email").value("marko@test.com"));
    }

    @Test
    @DisplayName("POST /api/owners - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateOwnerRequest(
                "", "", null, "",
                null, null, null, null, null);

        performPost("/api/owners", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/owners - get all success")
    void getAll_success() throws Exception {
        seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        seedOwner(clinicAId, "Ana", "Jovic", "+381642222222");

        performGet("/api/owners", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/owners/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");

        performGet("/api/owners/" + ownerId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownerId.toString()))
                .andExpect(jsonPath("$.firstName").value("Marko"))
                .andExpect(jsonPath("$.lastName").value("Petrovic"));
    }

    @Test
    @DisplayName("GET /api/owners/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/owners/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/owners/search/by-last-name - search success")
    void searchByLastName() throws Exception {
        seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        seedOwner(clinicAId, "Ana", "Jovic", "+381642222222");

        performGet("/api/owners/search/by-last-name?lastName=Petrovic", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastName").value("Petrovic"));
    }

    @Test
    @DisplayName("GET /api/owners/search/by-phone - search success")
    void searchByPhone() throws Exception {
        seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        seedOwner(clinicAId, "Ana", "Jovic", "+381642222222");

        performGet("/api/owners/search/by-phone?phone=+381641111111", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Marko"));
    }

    @Test
    @DisplayName("PUT /api/owners/{id} - update success")
    void update_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");

        var updateRequest = new UpdateOwnerRequest(
                "Marko", "Petrovic-Jankovic", null, null,
                null, null, null, null, null);

        performPut("/api/owners/" + ownerId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Petrovic-Jankovic"));
    }

    @Test
    @DisplayName("DELETE /api/owners/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");

        performDelete("/api/owners/" + ownerId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/owners/" + ownerId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/owners - tenant isolation: Clinic A cannot see Clinic B owners")
    void tenantIsolation() throws Exception {
        seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        seedOwner(clinicBId, "Ana", "Jovic", "+381642222222");

        performGet("/api/owners", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Marko"));

        performGet("/api/owners", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Ana"));
    }
}
