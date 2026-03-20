package com.softart.vetclinic.config.audit;

import com.softart.vetclinic.IntegrationTestBase;
import com.softart.vetclinic.dto.CreateOwnerRequest;
import com.softart.vetclinic.dto.LoginRequest;
import com.softart.vetclinic.dto.UpdateOwnerRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLoggingAspectTest extends IntegrationTestBase {

    @Test
    @DisplayName("CREATE operation produces audit log entry")
    void create_producesAuditLog() throws Exception {
        var request = new CreateOwnerRequest(
                "Marko", "Petrovic", "marko@test.com", "+381641234567",
                null, null, null, null, null);

        performPost("/api/owners", tokenA, clinicAId, request)
                .andExpect(status().isCreated());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE clinic_id = ? AND action = 'CREATE' AND entity_type = 'Owner'",
                    Integer.class, clinicAId);
            assert count != null && count == 1;
        });
    }

    @Test
    @DisplayName("UPDATE operation captures old and new values")
    void update_capturesOldAndNewValues() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");

        var updateRequest = new UpdateOwnerRequest(
                "Marko", "Jankovic", null, "+381641111111",
                null, null, null, null, null);

        performPut("/api/owners/" + ownerId, tokenA, clinicAId, updateRequest)
                .andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE clinic_id = ? AND action = 'UPDATE' AND entity_type = 'Owner' AND entity_id = ?",
                    Integer.class, clinicAId, ownerId);
            assert count != null && count == 1;

            String oldValues = jdbc.queryForObject(
                    "SELECT old_values FROM audit_log WHERE clinic_id = ? AND action = 'UPDATE' AND entity_id = ?",
                    String.class, clinicAId, ownerId);
            assert oldValues != null && oldValues.contains("Petrovic");

            String newValues = jdbc.queryForObject(
                    "SELECT new_values FROM audit_log WHERE clinic_id = ? AND action = 'UPDATE' AND entity_id = ?",
                    String.class, clinicAId, ownerId);
            assert newValues != null && newValues.contains("Jankovic");
        });
    }

    @Test
    @DisplayName("DELETE operation captures old values, no new values")
    void delete_capturesOldValues() throws Exception {
        UUID ownerId = seedOwner(clinicAId, "Marko", "Petrovic", "+381641111111");

        performDelete("/api/owners/" + ownerId, tokenA, clinicAId)
                .andExpect(status().isNoContent());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE clinic_id = ? AND action = 'DELETE' AND entity_type = 'Owner' AND entity_id = ?",
                    Integer.class, clinicAId, ownerId);
            assert count != null && count == 1;

            String oldValues = jdbc.queryForObject(
                    "SELECT old_values FROM audit_log WHERE clinic_id = ? AND action = 'DELETE' AND entity_id = ?",
                    String.class, clinicAId, ownerId);
            assert oldValues != null && oldValues.contains("Petrovic");
        });
    }

    @Test
    @DisplayName("Audit log records userId from JWT principal")
    void auditLog_capturesUserId() throws Exception {
        var request = new CreateOwnerRequest(
                "Ana", "Jovic", null, "+381649999999",
                null, null, null, null, null);

        performPost("/api/owners", tokenA, clinicAId, request)
                .andExpect(status().isCreated());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            UUID userId = jdbc.queryForObject(
                    "SELECT user_id FROM audit_log WHERE clinic_id = ? AND action = 'CREATE' AND entity_type = 'Owner'",
                    UUID.class, clinicAId);
            assert userAId.equals(userId);
        });
    }

    @Test
    @DisplayName("Audit logs are clinic-scoped (tenant isolation)")
    void tenantIsolation() throws Exception {
        var request = new CreateOwnerRequest(
                "Marko", "Petrovic", null, "+381641234567",
                null, null, null, null, null);

        performPost("/api/owners", tokenA, clinicAId, request)
                .andExpect(status().isCreated());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer countA = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE clinic_id = ?",
                    Integer.class, clinicAId);
            assert countA != null && countA > 0;
        });

        // Clinic B should see zero audit logs via API
        performGet("/api/audit-logs", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- LOGIN / LOGOUT ----

    @Test
    @DisplayName("LOGIN produces audit log with email in new_values")
    void login_producesAuditLog() throws Exception {
        var request = new LoginRequest(clinicAId, "admin@clinica.test", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE clinic_id = ? AND action = 'LOGIN' AND entity_type = 'Auth'",
                    Integer.class, clinicAId);
            assert count != null && count == 1;

            String newValues = jdbc.queryForObject(
                    "SELECT new_values FROM audit_log WHERE clinic_id = ? AND action = 'LOGIN'",
                    String.class, clinicAId);
            assert newValues != null && newValues.contains("admin@clinica.test");
        });
    }

    @Test
    @DisplayName("LOGOUT produces audit log entry")
    void logout_producesAuditLog() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Clinic-Id", clinicAId.toString()))
                .andExpect(status().isNoContent());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_log WHERE clinic_id = ? AND action = 'LOGOUT' AND entity_type = 'Auth'",
                    Integer.class, clinicAId);
            assert count != null && count == 1;

            UUID userId = jdbc.queryForObject(
                    "SELECT user_id FROM audit_log WHERE clinic_id = ? AND action = 'LOGOUT'",
                    UUID.class, clinicAId);
            assert userAId.equals(userId);
        });
    }
}
