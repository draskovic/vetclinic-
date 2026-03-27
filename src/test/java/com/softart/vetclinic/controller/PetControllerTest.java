package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreatePetRequest;
import com.softart.vetclinic.dto.UpdatePetRequest;
import com.softart.vetclinic.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PetControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/pets - create success with ownerName populated")
    void create_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");

        var request = new CreatePetRequest(
                ownerId, null, null, "Rex",
                LocalDate.of(2020, 5, 15), Gender.MALE, "Brown",
                new BigDecimal("25.50"), null, false, false,
                null, null, null, null, null,null);

        performPost("/api/pets", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.ownerName").isNotEmpty())
                .andExpect(jsonPath("$.gender").value("MALE"));
    }

    @Test
    @DisplayName("POST /api/pets - create with species and breed populates names")
    void create_withOptionalFields() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        UUID speciesId = seedSpecies(clinicAId, "Dog");
        UUID breedId = seedBreed(clinicAId, speciesId, "Labrador");

        var request = new CreatePetRequest(
                ownerId, speciesId, breedId, "Buddy",
                null, null, null, null, null,
                false, false, null, null, null, null, null,null);

        performPost("/api/pets", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Buddy"))
                .andExpect(jsonPath("$.speciesName").value("Dog"))
                .andExpect(jsonPath("$.breedName").value("Labrador"));
    }

    @Test
    @DisplayName("POST /api/pets - invalid owner returns 404")
    void create_invalidOwner() throws Exception {
        var request = new CreatePetRequest(
                UUID.randomUUID(), null, null, "Rex",
                null, null, null, null, null,
                false, false, null, null, null, null, null,null);

        performPost("/api/pets", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/pets - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreatePetRequest(
                null, null, null, "",
                null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        performPost("/api/pets", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/pets - get all success")
    void getAll_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        seedPet(clinicAId, ownerId, "Rex");
        seedPet(clinicAId, ownerId, "Buddy");

        performGet("/api/pets", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/pets/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        UUID petId = seedPet(clinicAId, ownerId, "Rex");

        performGet("/api/pets/" + petId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(petId.toString()))
                .andExpect(jsonPath("$.name").value("Rex"));
    }

    @Test
    @DisplayName("GET /api/pets/by-owner/{ownerId} - get pets by owner")
    void getByOwner() throws Exception {
        UUID owner1 = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        UUID owner2 = seedOwner(clinicAId, "Ana", "Jovic", "+381642222222");
        seedPet(clinicAId, owner1, "Rex");
        seedPet(clinicAId, owner1, "Buddy");
        seedPet(clinicAId, owner2, "Miki");

        performGet("/api/pets/by-owner/" + owner1, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Rex", "Buddy")));
    }

    @Test
    @DisplayName("PUT /api/pets/{id} - update success")
    void update_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        UUID petId = seedPet(clinicAId, ownerId, "Rex");

        var updateRequest = new UpdatePetRequest(
                null, null, null, "Rex Jr.",
                null, null, null, null, null,
                null, null, null, null, null, null, null,null);

        performPut("/api/pets/" + petId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rex Jr."));
    }

    @Test
    @DisplayName("DELETE /api/pets/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        UUID petId = seedPet(clinicAId, ownerId, "Rex");

        performDelete("/api/pets/" + petId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/pets/" + petId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/pets - tenant isolation: Clinic A cannot see Clinic B pets")
    void tenantIsolation() throws Exception {
        UUID ownerA = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");
        seedPet(clinicAId, ownerA, "Rex");

        UUID ownerB = seedOwner(clinicBId, "Ana", "Jovic", "+381642222222");
        seedPet(clinicBId, ownerB, "Miki");

        performGet("/api/pets", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Rex"));

        performGet("/api/pets", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Miki"));
    }
}
