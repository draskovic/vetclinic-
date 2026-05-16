package com.softart.vetclinic.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateInvoiceRequest;
import com.softart.vetclinic.enums.InvoiceStatus;

class InvoiceControllerTest extends IntegrationTestBase {

    private UUID ownerId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Milan", "Milanovic", "+381613333333");
    }

    @Test
    @DisplayName("POST /api/invoices - create success (auto-generates invoice number)")
    void create_success() throws Exception {
        seedPrerequisites();

        var request = new CreateInvoiceRequest(null, ownerId, null, null,
                InvoiceStatus.DRAFT, null, null,
                new BigDecimal("100.00"), new BigDecimal("20.00"), BigDecimal.ZERO,
                new BigDecimal("120.00"), "RSD", null,null);

        performPost("/api/invoices", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /api/invoices - invalid owner returns 404")
    void create_invalidOwner() throws Exception {
        var request = new CreateInvoiceRequest(null, UUID.randomUUID(),null, null,
                InvoiceStatus.DRAFT, null, null,  null,null, null, null, null, null,null);

        performPost("/api/invoices", tokenA, clinicAId, request)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/invoices - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreateInvoiceRequest(null, null, null,null,
                null, null, null, null, null, null, null, null, null, null);

        performPost("/api/invoices", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/invoices - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedInvoice(clinicAId, ownerId, "INV-001", "DRAFT");

        performGet("/api/invoices", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/invoices/{id} - get by ID success")
    void getById_success() throws Exception {
        seedPrerequisites();
        UUID invoiceId = seedInvoice(clinicAId, ownerId, "INV-001", "DRAFT");

        performGet("/api/invoices/" + invoiceId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
    }

    @Test
    @DisplayName("GET /api/invoices/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/invoices/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/invoices/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID invoiceId = seedInvoice(clinicAId, ownerId, "INV-002", "DRAFT");

        performDelete("/api/invoices/" + invoiceId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/invoices/" + invoiceId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/invoices/by-owner/{ownerId} - filter by owner")
    void getByOwner_success() throws Exception {
        seedPrerequisites();
        UUID owner2Id = seedOwner(clinicAId, "Nikola", "Nikolic", "+381614444444");
        seedInvoice(clinicAId, ownerId, "INV-001", "DRAFT");
        seedInvoice(clinicAId, ownerId, "INV-002", "PAID");
        seedInvoice(clinicAId, owner2Id, "INV-003", "DRAFT");

        performGet("/api/invoices/by-owner/" + ownerId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/invoices/by-status/{status} - filter by status")
    void getByStatus_success() throws Exception {
        seedPrerequisites();
        seedInvoice(clinicAId, ownerId, "INV-001", "DRAFT");
        seedInvoice(clinicAId, ownerId, "INV-002", "PAID");
        seedInvoice(clinicAId, ownerId, "INV-003", "DRAFT");

        performGet("/api/invoices/by-status/DRAFT", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/invoices - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedInvoice(clinicAId, ownerId, "INV-001", "DRAFT");

        performGet("/api/invoices", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        performGet("/api/invoices", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
