package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateBreedRequest;
import com.softart.vetclinic.dto.UpdateBreedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BreedControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/breeds - create success with speciesName populated")
    void create_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");

        var request = new CreateBreedRequest(speciesId, "Labrador");

        performPost("/api/breeds", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Labrador"))
                .andExpect(jsonPath("$.speciesId").value(speciesId.toString()))
                .andExpect(jsonPath("$.speciesName").value("Dog"));
    }

    @Test
    @DisplayName("POST /api/breeds - invalid species returns 404")
    void create_invalidSpecies() throws Exception {
        var request = new CreateBreedRequest(UUID.randomUUID(), "Labrador");

        performPost("/api/breeds", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/breeds - duplicate name for same species returns 409")
    void create_duplicateName() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");
        seedBreed(clinicAId, speciesId, "Labrador");

        var request = new CreateBreedRequest(speciesId, "Labrador");

        performPost("/api/breeds", tokenA, clinicAId, request)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/breeds - blank fields returns 400")
    void create_blankFields() throws Exception {
        performPost("/api/breeds", tokenA, clinicAId, new CreateBreedRequest(null, ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/breeds - get all success")
    void getAll_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");
        seedBreed(clinicAId, speciesId, "Labrador");
        seedBreed(clinicAId, speciesId, "Poodle");

        performGet("/api/breeds", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].speciesName", everyItem(is("Dog"))));
    }

    @Test
    @DisplayName("GET /api/breeds/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");
        UUID breedId = seedBreed(clinicAId, speciesId, "Labrador");

        performGet("/api/breeds/" + breedId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(breedId.toString()))
                .andExpect(jsonPath("$.name").value("Labrador"))
                .andExpect(jsonPath("$.speciesName").value("Dog"));
    }

    @Test
    @DisplayName("GET /api/breeds/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/breeds/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/breeds/by-species/{speciesId} - get breeds by species")
    void getBySpecies() throws Exception {
        UUID dogId = seedSpecies(clinicAId, "Dog");
        UUID catId = seedSpecies(clinicAId, "Cat");
        seedBreed(clinicAId, dogId, "Labrador");
        seedBreed(clinicAId, dogId, "Poodle");
        seedBreed(clinicAId, catId, "Persian");

        performGet("/api/breeds/by-species/" + dogId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Labrador", "Poodle")));
    }

    @Test
    @DisplayName("PUT /api/breeds/{id} - update success")
    void update_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");
        UUID breedId = seedBreed(clinicAId, speciesId, "Labrador");

        var updateRequest = new UpdateBreedRequest(null, "Labrador Retriever");

        performPut("/api/breeds/" + breedId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Labrador Retriever"));
    }

    @Test
    @DisplayName("DELETE /api/breeds/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID speciesId = seedSpecies(clinicAId, "Dog");
        UUID breedId = seedBreed(clinicAId, speciesId, "Labrador");

        performDelete("/api/breeds/" + breedId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/breeds/" + breedId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/breeds - tenant isolation: Clinic A cannot see Clinic B breeds")
    void tenantIsolation() throws Exception {
        UUID dogA = seedSpecies(clinicAId, "Dog");
        seedBreed(clinicAId, dogA, "Labrador");

        UUID dogB = seedSpecies(clinicBId, "Dog");
        seedBreed(clinicBId, dogB, "Poodle");

        performGet("/api/breeds", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Labrador"));

        performGet("/api/breeds", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Poodle"));
    }

    @Test
    @DisplayName("POST /api/breeds - species from other clinic returns 404")
    void create_speciesFromOtherClinic() throws Exception {
        UUID speciesBId = seedSpecies(clinicBId, "Dog");

        var request = new CreateBreedRequest(speciesBId, "Labrador");

        performPost("/api/breeds", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }
}
