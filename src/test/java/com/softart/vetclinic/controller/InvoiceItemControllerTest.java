package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateInvoiceItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InvoiceItemControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID invoiceId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Dragan", "Dragic", "+381617777777");
        invoiceId = seedInvoice(clinicAId, ownerId, "INV-100", "DRAFT");
    }

    @Test
    @DisplayName("POST /api/invoice-items - create success")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateInvoiceItemRequest(invoiceId, null, "General Checkup",
                new BigDecimal("1"), new BigDecimal("50.00"), null, BigDecimal.ZERO,
                new BigDecimal("50.00"), 1);

        performPost("/api/invoice-items", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("General Checkup"));
    }

    @Test
    @DisplayName("POST /api/invoice-items - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateInvoiceItemRequest(null, null, "",
                null, null, null, null, null, null);

        performPost("/api/invoice-items", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/invoice-items - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedInvoiceItem(clinicAId, invoiceId, "Checkup", new BigDecimal("50.00"));

        performGet("/api/invoice-items", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/invoice-items/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/invoice-items/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/invoice-items/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID itemId = seedInvoiceItem(clinicAId, invoiceId, "Temp", new BigDecimal("10.00"));

        performDelete("/api/invoice-items/" + itemId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/invoice-items/by-invoice/{invoiceId} - filter by invoice")
    void getByInvoice_success() throws Exception {
        seedPrerequisites();
        UUID invoice2Id = seedInvoice(clinicAId, ownerId, "INV-101", "DRAFT");
        seedInvoiceItem(clinicAId, invoiceId, "Item 1", new BigDecimal("50.00"));
        seedInvoiceItem(clinicAId, invoiceId, "Item 2", new BigDecimal("30.00"));
        seedInvoiceItem(clinicAId, invoice2Id, "Item 3", new BigDecimal("20.00"));

        performGet("/api/invoice-items/by-invoice/" + invoiceId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/invoice-items - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedInvoiceItem(clinicAId, invoiceId, "Item A", new BigDecimal("50.00"));

        performGet("/api/invoice-items", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedInvoiceItem(UUID clinicId, UUID invoiceId, String description, BigDecimal unitPrice) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO invoice_item (id, clinic_id, invoice_id, description, quantity, unit_price, " +
                "tax_rate_id, tax_rate_label, tax_rate_percent, " +
                "discount_percent, line_total, sort_order, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, 1, ?, " +
                "(SELECT id FROM tax_rate WHERE country_code='RS' AND label='Ђ' LIMIT 1), 'Ђ', 20.00, " +
                "0, ?, 0, NOW(), NOW(), false, 0)",
                id, clinicId, invoiceId, description, unitPrice, unitPrice);
        return id;
    }
}
