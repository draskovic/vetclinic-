package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateServiceRequest;
import com.softart.vetclinic.dto.UpdateServiceRequest;
import com.softart.vetclinic.enums.ServiceCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClinicServiceControllerTest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/services - create success")
    void create_success() throws Exception {
        var request = new CreateServiceRequest(ServiceCategory.EXAMINATION, "General Checkup",null, null,
                "Full body examination", new BigDecimal("50.00"), new BigDecimal("20.00"), 30, true);

        performPost("/api/services", tokenA, clinicAId, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("General Checkup"))
                .andExpect(jsonPath("$.category").value("EXAMINATION"))
                .andExpect(jsonPath("$.price").value(50.00));
    }

    @Test
    @DisplayName("POST /api/services - duplicate name returns 409")
    void create_duplicateName() throws Exception {
        seedService(clinicAId, "EXAMINATION", "Checkup", new BigDecimal("30.00"));

        var request = new CreateServiceRequest(ServiceCategory.EXAMINATION, "Checkup",null,null,
                null, new BigDecimal("40.00"), null, null, null);

        performPost("/api/services", tokenA, clinicAId, request)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/services - blank name returns 400")
    void create_blankName() throws Exception {
        var request = new CreateServiceRequest(ServiceCategory.EXAMINATION, "",null,null,
                null, new BigDecimal("50.00"), null, null, null);

        performPost("/api/services", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/services - null category returns 400")
    void create_nullCategory() throws Exception {
        var request = new CreateServiceRequest(null, "Checkup",null,null,
                null, new BigDecimal("50.00"), null, null, null);

        performPost("/api/services", tokenA, clinicAId, request)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/services - get all success")
    void getAll_success() throws Exception {
        seedService(clinicAId, "EXAMINATION", "Checkup", new BigDecimal("30.00"));
        seedService(clinicAId, "SURGERY", "Spay", new BigDecimal("200.00"));

        performGet("/api/services", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/services/{id} - get by ID success")
    void getById_success() throws Exception {
        UUID serviceId = seedService(clinicAId, "DENTAL", "Teeth Cleaning", new BigDecimal("80.00"));

        performGet("/api/services/" + serviceId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId.toString()))
                .andExpect(jsonPath("$.name").value("Teeth Cleaning"));
    }

    @Test
    @DisplayName("GET /api/services/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/services/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/services/{id} - update success")
    void update_success() throws Exception {
        UUID serviceId = seedService(clinicAId, "EXAMINATION", "Checkup", new BigDecimal("30.00"));

        var updateRequest = new UpdateServiceRequest(null, "Advanced Checkup",null,null,
                null, new BigDecimal("60.00"), null, null, null);

        performPut("/api/services/" + serviceId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Advanced Checkup"));
    }

    @Test
    @DisplayName("DELETE /api/services/{id} - soft delete success")
    void delete_success() throws Exception {
        UUID serviceId = seedService(clinicAId, "GROOMING", "Bath", new BigDecimal("25.00"));

        performDelete("/api/services/" + serviceId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        performGet("/api/services/" + serviceId, tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/services/by-category/{category} - filter by category")
    void getByCategory_success() throws Exception {
        seedService(clinicAId, "EXAMINATION", "Checkup", new BigDecimal("30.00"));
        seedService(clinicAId, "SURGERY", "Spay", new BigDecimal("200.00"));
        seedService(clinicAId, "EXAMINATION", "Blood Test", new BigDecimal("40.00"));

        performGet("/api/services/by-category/EXAMINATION", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/services - tenant isolation")
    void tenantIsolation() throws Exception {
        seedService(clinicAId, "EXAMINATION", "Checkup A", new BigDecimal("30.00"));
        seedService(clinicBId, "EXAMINATION", "Checkup B", new BigDecimal("35.00"));

        performGet("/api/services", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Checkup A"));
    }
}
