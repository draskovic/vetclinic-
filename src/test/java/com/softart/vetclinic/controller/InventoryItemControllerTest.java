package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateInventoryItemRequest;
import com.softart.vetclinic.enums.InventoryCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryItemControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/inventory-items - create success")
    void create_success() throws Exception {
        var request = new CreateInventoryItemRequest(null, "Amoxicillin 500mg",
                "AMX-500", InventoryCategory.MEDICATION, new BigDecimal("100"),
                "tablets", new BigDecimal("20"), new BigDecimal("5.00"),
                new BigDecimal("10.00"), null, true);

        performPost("/api/inventory-items", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Amoxicillin 500mg"))
                .andExpect(jsonPath("$.category").value("MEDICATION"));
    }

    @Test
    @DisplayName("POST /api/inventory-items - blank name returns 400")
    void create_blankName() throws Exception {
        var request = new CreateInventoryItemRequest(null, "",
                null, InventoryCategory.SUPPLY, null, null, null, null, null, null, null);

        performPost("/api/inventory-items", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/inventory-items - null category returns 400")
    void create_nullCategory() throws Exception {
        var request = new CreateInventoryItemRequest(null, "Test Item",
                null, null, null, null, null, null, null, null, null);

        performPost("/api/inventory-items", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/inventory-items - get all success")
    void getAll_success() throws Exception {
        seedInventoryItem(clinicAId, "Syringe", "SUPPLY");
        seedInventoryItem(clinicAId, "Bandage", "SUPPLY");

        performGet("/api/inventory-items", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/inventory-items/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID itemId = seedInventoryItem(clinicAId, "Vaccine Vial", "MEDICATION");

        performGet("/api/inventory-items/" + itemId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vaccine Vial"));
    }

    @Test
    @DisplayName("GET /api/inventory-items/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/inventory-items/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/inventory-items/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID itemId = seedInventoryItem(clinicAId, "Temp Item", "EQUIPMENT");

        performDelete("/api/inventory-items/" + itemId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/inventory-items/" + itemId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/inventory-items/by-category/{category} - filter by category")
    void getByCategory_success() throws Exception {
        seedInventoryItem(clinicAId, "Amoxicillin", "MEDICATION");
        seedInventoryItem(clinicAId, "Syringe", "SUPPLY");
        seedInventoryItem(clinicAId, "Doxycycline", "MEDICATION");

        performGet("/api/inventory-items/by-category/MEDICATION", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/inventory-items - tenant isolation")
    void tenantIsolation() throws Exception {
        seedInventoryItem(clinicAId, "Item A", "SUPPLY");
        seedInventoryItem(clinicBId, "Item B", "SUPPLY");

        performGet("/api/inventory-items", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Item A"));
    }
}
