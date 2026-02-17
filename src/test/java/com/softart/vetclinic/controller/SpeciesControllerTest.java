package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateSpeciesRequest;
import com.softart.vetclinic.dto.UpdateSpeciesRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SpeciesControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/species - create success")
    void create_success() throws Exception {
        var request = new CreateSpeciesRequest("Dog", true);

        performPost("/api/species", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Dog"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/species - duplicate name returns 409")
    void create_duplicateName() throws Exception {
        seedSpecies(clinicAId, "Cat");

        var request = new CreateSpeciesRequest("Cat", true);

        performPost("/api/species", tokenA, clinicAId, request)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/species - blank name returns 400")
    void create_blankName() throws Exception {
        var request = new CreateSpeciesRequest("", true);

        performPost("/api/species", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/species - unauthenticated returns 403")
    void create_unauthenticated() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/species")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("X-Clinic-Id", clinicAId.toString())
                        .content(objectMapper.writeValueAsString(new CreateSpeciesRequest("Dog", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/species - get all success")
    void getAll_success() throws Exception {
        seedSpecies(clinicAId, "Dog");
        seedSpecies(clinicAId, "Cat");

        performGet("/api/species", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/species - empty result")
    void getAll_emptyResult() throws Exception {
        performGet("/api/species", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/species - pagination")
    void getAll_pagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            seedSpecies(clinicAId, "Species" + i);
        }

        performGet("/api/species?page=0&size=2&sort=name,asc", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("GET /api/species/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");

        performGet("/api/species/" + speciesId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(speciesId.toString()))
                .andExpect(jsonPath("$.name").value("Dog"));
    }

    @Test
    @DisplayName("GET /api/species/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/species/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/species/{id} - update success")
    void update_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");

        var updateRequest = new UpdateSpeciesRequest("Canine", null);

        performPut("/api/species/" + speciesId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Canine"));
    }

    @Test
    @DisplayName("DELETE /api/species/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");

        performDelete("/api/species/" + speciesId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        // Verify it's gone from normal queries
        performGet("/api/species/" + speciesId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/species - tenant isolation: Clinic A cannot see Clinic B data")
    void tenantIsolation() throws Exception {
        seedSpecies(clinicAId, "Dog");
        seedSpecies(clinicBId, "Cat");

        // Clinic A should only see its own species
        performGet("/api/species", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Dog"));

        // Clinic B should only see its own species
        performGet("/api/species", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Cat"));
    }
}
