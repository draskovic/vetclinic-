package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreatePaymentRequest;
import com.softart.vetclinic.enums.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest extends IntegrationTestBase {

    private UUID ownerId;
    private UUID invoiceId;

    private void seedPrerequisites() {
        ownerId = seedOwner(clinicAId, "Nikola", "Nikolic", "+381618888888");
        invoiceId = seedInvoice(clinicAId, ownerId, "INV-200", "ISSUED");
    }

    @Test
    @DisplayName("POST /api/payments - create success")
    void create_success() throws Exception {
        seedPrerequisites();
        OffsetDateTime paidAt = OffsetDateTime.of(2026, 2, 5, 14, 0, 0, 0, ZoneOffset.UTC);

        var request = new CreatePaymentRequest(invoiceId, new BigDecimal("120.00"),
                PaymentMethod.CASH, paidAt, null, "Full payment");

        performPost("/api/payments", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.method").value("CASH"));
    }

    @Test
    @DisplayName("POST /api/payments - missing required fields returns 400")
    void create_missingRequired() throws Exception {
        var request = new CreatePaymentRequest(null, null, null, null, null, null);

        performPost("/api/payments", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/payments - get all success")
    void getAll_success() throws Exception {
        seedPrerequisites();
        seedPayment(clinicAId, invoiceId, new BigDecimal("100.00"), "CASH");

        performGet("/api/payments", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/payments/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/payments/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/payments/{id} - soft delete success")
    void delete_success() throws Exception {
        seedPrerequisites();
        UUID paymentId = seedPayment(clinicAId, invoiceId, new BigDecimal("50.00"), "CARD");

        performDelete("/api/payments/" + paymentId, tokenA, clinicAId)
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/payments/by-invoice/{invoiceId} - filter by invoice")
    void getByInvoice_success() throws Exception {
        seedPrerequisites();
        UUID invoice2Id = seedInvoice(clinicAId, ownerId, "INV-201", "ISSUED");
        seedPayment(clinicAId, invoiceId, new BigDecimal("60.00"), "CASH");
        seedPayment(clinicAId, invoiceId, new BigDecimal("40.00"), "CARD");
        seedPayment(clinicAId, invoice2Id, new BigDecimal("80.00"), "TRANSFER");

        performGet("/api/payments/by-invoice/" + invoiceId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/payments - tenant isolation")
    void tenantIsolation() throws Exception {
        seedPrerequisites();
        seedPayment(clinicAId, invoiceId, new BigDecimal("100.00"), "CASH");

        performGet("/api/payments", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private UUID seedPayment(UUID clinicId, UUID invoiceId, BigDecimal amount, String method) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO payment (id, clinic_id, invoice_id, amount, method, paid_at, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), NOW(), false, 0)",
                id, clinicId, invoiceId, amount, method);
        return id;
    }
}
