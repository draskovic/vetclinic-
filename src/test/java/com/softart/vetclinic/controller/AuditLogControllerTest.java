package com.softart.vetclinic.controller;

import com.softart.vetclinic.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLogControllerTest extends IntegrationTestBase {

    private UUID auditLogId;
    private UUID entityId1;

    private void seedAuditLogs() {
        auditLogId = UUID.randomUUID();
        entityId1 = UUID.randomUUID();

        // Clinic A: CREATE Owner
        jdbc.update(
                "INSERT INTO audit_log (id, clinic_id, user_id, action, entity_type, entity_id, new_values, ip_address, user_agent, created_at) " +
                        "VALUES (?, ?, ?, 'CREATE', 'Owner', ?, '{\"firstName\":\"Marko\"}'::jsonb, '127.0.0.1', 'TestAgent', NOW())",
                auditLogId, clinicAId, userAId, entityId1);

        // Clinic A: UPDATE Pet
        jdbc.update(
                "INSERT INTO audit_log (id, clinic_id, user_id, action, entity_type, entity_id, old_values, new_values, ip_address, created_at) " +
                        "VALUES (?, ?, ?, 'UPDATE', 'Pet', ?, '{\"name\":\"Rex\"}'::jsonb, '{\"name\":\"Max\"}'::jsonb, '192.168.1.1', NOW())",
                UUID.randomUUID(), clinicAId, userAId, UUID.randomUUID());

        // Clinic A: DELETE Species (2 days ago)
        jdbc.update(
                "INSERT INTO audit_log (id, clinic_id, user_id, action, entity_type, entity_id, old_values, created_at) " +
                        "VALUES (?, ?, ?, 'DELETE', 'Species', ?, '{\"name\":\"Pas\"}'::jsonb, ?)",
                UUID.randomUUID(), clinicAId, userAId, UUID.randomUUID(),
                OffsetDateTime.now().minusDays(2));

        // Clinic B: CREATE Owner (should NOT be visible to Clinic A)
        jdbc.update(
                "INSERT INTO audit_log (id, clinic_id, user_id, action, entity_type, entity_id, created_at) " +
                        "VALUES (?, ?, ?, 'CREATE', 'Owner', ?, NOW())",
                UUID.randomUUID(), clinicBId, userBId, UUID.randomUUID());
    }

    @Test
    @DisplayName("GET /api/audit-logs - returns paginated list for clinic")
    void getAll_success() throws Exception {
        seedAuditLogs();

        performGet("/api/audit-logs", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("GET /api/audit-logs/{id} - returns single entry")
    void getById_success() throws Exception {
        seedAuditLogs();

        performGet("/api/audit-logs/" + auditLogId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auditLogId.toString()))
                .andExpect(jsonPath("$.action").value("CREATE"))
                .andExpect(jsonPath("$.entityType").value("Owner"));
    }

    @Test
    @DisplayName("GET /api/audit-logs/{id} - not found returns 404")
    void getById_notFound() throws Exception {
        performGet("/api/audit-logs/" + UUID.randomUUID(), tokenA, clinicAId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/audit-logs/by-action/CREATE - filters by action")
    void getByAction_success() throws Exception {
        seedAuditLogs();

        performGet("/api/audit-logs/by-action/CREATE", tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
    }

    @Test
    @DisplayName("GET /api/audit-logs/by-user/{userId} - filters by user")
    void getByUser_success() throws Exception {
        seedAuditLogs();

        performGet("/api/audit-logs/by-user/" + userAId, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @DisplayName("GET /api/audit-logs/by-entity/{entityType}/{entityId} - filters by entity")
    void getByEntity_success() throws Exception {
        seedAuditLogs();

        performGet("/api/audit-logs/by-entity/Owner/" + entityId1, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].entityType").value("Owner"));
    }

    @Test
    @DisplayName("GET /api/audit-logs/by-date-range - filters by date")
    void getByDateRange_success() throws Exception {
        seedAuditLogs();

        String from = OffsetDateTime.now().minusDays(3).toString();
        String to = OffsetDateTime.now().plusDays(1).toString();

        performGet("/api/audit-logs/by-date-range?from=" + from + "&to=" + to, tokenA, clinicAId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @DisplayName("GET /api/audit-logs - tenant isolation")
    void tenantIsolation() throws Exception {
        seedAuditLogs();

        performGet("/api/audit-logs", tokenB, clinicBId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].entityType").value("Owner"));
    }
}
