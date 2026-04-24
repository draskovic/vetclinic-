package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateInventoryTransactionRequest;
import com.softart.vetclinic.enums.InventoryTransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryTransactionControllerTest extends IntegrationTestBase {

    private UUID itemId;

    private void seedPrerequisites() {
        itemId = seedInventoryItem(clinicAId, "Syringe", "SUPPLY");
    }

    @Test
    @DisplayName("POST /api/inventory-transactions - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateInventoryTransactionRequest(itemId,
                InventoryTransactionType.IN, new BigDecimal("50"),
                null, null,null, userAId, "Restocking",null);

        performPost("/api/inventory-transactions", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("IN"));
    }

    @Test
    @DisplayName("POST /api/inventory-transactions - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateInventoryTransactionRequest(null, null, null,
                null, null,null, null, null, null);

        performPost("/api/inventory-transactions", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/inventory-transactions - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedInventoryTransaction(clinicAId, itemId, "IN", new BigDecimal("50"));

        performGet("/api/inventory-transactions", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/inventory-transactions/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/inventory-transactions/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/inventory-transactions/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID txId = seedInventoryTransaction(clinicAId, itemId, "OUT", new BigDecimal("10"));

        performDelete("/api/inventory-transactions/" + txId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/inventory-transactions/by-item/{inventoryItemId} - filter by item")
    void getByItem_success() throws Exception {
        seedPrerequisites();
        UUID item2Id = seedInventoryItem(clinicAId, "Bandage", "SUPPLY");
        seedInventoryTransaction(clinicAId, itemId, "IN", new BigDecimal("50"));
        seedInventoryTransaction(clinicAId, itemId, "OUT", new BigDecimal("10"));
        seedInventoryTransaction(clinicAId, item2Id, "IN", new BigDecimal("30"));

        performGet("/api/inventory-transactions/by-item/" + itemId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/inventory-transactions - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedInventoryTransaction(clinicAId, itemId, "IN", new BigDecimal("50"));

        performGet("/api/inventory-transactions", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedInventoryTransaction(UUID clinicId, UUID inventoryItemId, String type, BigDecimal quantity) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO inventory_transaction (id, clinic_id, inventory_item_id, type, quantity, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), false, 0)",
                id, clinicId, inventoryItemId, type, quantity);
        return id;
    }
}
