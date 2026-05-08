package com.softart.vetclinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softart.vetclinic.config.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(scripts = "/test-init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("vetapp_test")
            .withUsername("postgres")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbc;

    // Clinic A data
    protected UUID clinicAId;
    protected UUID roleAId;
    protected UUID userAId;
    protected String tokenA;

    // Clinic B data (for tenant isolation tests)
    protected UUID clinicBId;
    protected UUID roleBId;
    protected UUID userBId;
    protected String tokenB;

    @BeforeEach
    void setUp() {
        truncateAllTables();
        seedTestData();
    }

    private void truncateAllTables() {
        jdbc.execute("TRUNCATE TABLE audit_log, document, notification, " +
                "inventory_transaction, inventory_item, user_location, payment, invoice_item, invoice, " +
                "prescription, vaccination, treatment, medical_record, appointment, pet, owner, " +
                "breed, species, service, clinic_location, refresh_token, users, role, clinic CASCADE");
    }

    private void seedTestData() {
        String encodedPassword = passwordEncoder.encode("password123");

        // Clinic A
        clinicAId = UUID.randomUUID();
        jdbc.update("INSERT INTO clinic (id, name, email, phone, city, country, subscription_plan, active, settings, created_at, updated_at, deleted, version) " +
                "VALUES (?, 'Clinic A', 'clinica@test.com', '+381111111111', 'Beograd', 'Serbia', 'BASIC', true, '{}'::jsonb, NOW(), NOW(), false, 0)",
                clinicAId);

        roleAId = UUID.randomUUID();
        jdbc.update("INSERT INTO role (id, clinic_id, name, permissions, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, 'ADMIN', '[\"*\"]'::jsonb, NOW(), NOW(), false, 0)",
                roleAId, clinicAId);

        userAId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, clinic_id, role_id, first_name, last_name, email, password_hash, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, 'Admin', 'ClinicA', ?, ?, true, NOW(), NOW(), false, 0)",
                userAId, clinicAId, roleAId, "admin@clinica.test", encodedPassword);

        tokenA = jwtService.generateAccessToken(userAId, clinicAId, "admin@clinica.test", "ADMIN", "[\"*\"]");

        // Clinic B
        clinicBId = UUID.randomUUID();
        jdbc.update("INSERT INTO clinic (id, name, email, phone, city, country, subscription_plan, active, settings, created_at, updated_at, deleted, version) " +
                "VALUES (?, 'Clinic B', 'clinicb@test.com', '+381222222222', 'Novi Sad', 'Serbia', 'BASIC', true, '{}'::jsonb, NOW(), NOW(), false, 0)",
                clinicBId);

        roleBId = UUID.randomUUID();
        jdbc.update("INSERT INTO role (id, clinic_id, name, permissions, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, 'ADMIN', '[\"*\"]'::jsonb, NOW(), NOW(), false, 0)",
                roleBId, clinicBId);

        userBId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, clinic_id, role_id, first_name, last_name, email, password_hash, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, 'Admin', 'ClinicB', ?, ?, true, NOW(), NOW(), false, 0)",
                userBId, clinicBId, roleBId, "admin@clinicb.test", encodedPassword);

        tokenB = jwtService.generateAccessToken(userBId, clinicBId, "admin@clinicb.test", "ADMIN", "[\"*\"]");
    }

    // ---- Helper methods for HTTP calls ----

    protected ResultActions performGet(String url, String token, UUID clinicId) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token)
                .header("X-Clinic-Id", clinicId.toString())
                .contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPost(String url, String token, UUID clinicId, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .header("X-Clinic-Id", clinicId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performPut(String url, String token, UUID clinicId, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .header("Authorization", "Bearer " + token)
                .header("X-Clinic-Id", clinicId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performDelete(String url, String token, UUID clinicId) throws Exception {
        return mockMvc.perform(delete(url)
                .header("Authorization", "Bearer " + token)
                .header("X-Clinic-Id", clinicId.toString())
                .contentType(MediaType.APPLICATION_JSON));
    }

    // ---- Helper methods to seed test entities ----

    protected UUID seedSpecies(UUID clinicId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO species (id, clinic_id, name, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, true, NOW(), NOW(), false, 0)", id, clinicId, name);
        return id;
    }

    protected UUID seedBreed(UUID clinicId, UUID speciesId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO breed (id, clinic_id, species_id, name, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, NOW(), NOW(), false, 0)", id, clinicId, speciesId, name);
        return id;
    }

    protected UUID seedOwner(UUID clinicId, String firstName, String lastName, String phone) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO owner (id, clinic_id, first_name, last_name, phone, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), false, 0)", id, clinicId, firstName, lastName, phone);
        return id;
    }

    protected UUID seedPet(UUID clinicId, UUID ownerId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO pet (id, clinic_id, owner_id, name, is_neutered, is_deceased, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, false, false, NOW(), NOW(), false, 0)", id, clinicId, ownerId, name);
        return id;
    }

    protected UUID seedRefreshToken(UUID userId, String token, boolean revoked, OffsetDateTime expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO refresh_token (id, user_id, token, revoked, expires_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())", id, userId, token, revoked, expiresAt);
        return id;
    }

    protected UUID seedService(UUID clinicId, String category, String name, java.math.BigDecimal price) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO service (id, clinic_id, category, name, price, tax_rate_id, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, " +
                "(SELECT id FROM tax_rate WHERE country_code='RS' AND label='Ђ' LIMIT 1), " +
                "true, NOW(), NOW(), false, 0)",
                id, clinicId, category, name, price);
        return id;
    }

    protected UUID seedClinicLocation(UUID clinicId, String name, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO clinic_location (id, clinic_id, name, is_main, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, false, ?, NOW(), NOW(), false, 0)", id, clinicId, name, active);
        return id;
    }

    protected UUID seedRole(UUID clinicId, String name, String permissions) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO role (id, clinic_id, name, permissions, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?::jsonb, NOW(), NOW(), false, 0)", id, clinicId, name, permissions);
        return id;
    }

    protected UUID seedUser(UUID clinicId, UUID roleId, String email, String firstName, String lastName) {
        UUID id = UUID.randomUUID();
        String encoded = passwordEncoder.encode("password123");
        jdbc.update("INSERT INTO users (id, clinic_id, role_id, first_name, last_name, email, password_hash, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, true, NOW(), NOW(), false, 0)", id, clinicId, roleId, firstName, lastName, email, encoded);
        return id;
    }

    protected UUID seedAppointment(UUID clinicId, UUID locationId, UUID petId, UUID ownerId, UUID vetId,
                                    OffsetDateTime startTime, OffsetDateTime endTime, String status, String type) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO appointment (id, clinic_id, location_id, pet_id, owner_id, vet_id, start_time, end_time, status, type, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), false, 0)",
                id, clinicId, locationId, petId, ownerId, vetId, startTime, endTime, status, type);
        return id;
    }

    protected UUID seedMedicalRecord(UUID clinicId, UUID petId, UUID vetId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO medical_record (id, clinic_id, pet_id, vet_id, follow_up_recommended, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, false, NOW(), NOW(), false, 0)", id, clinicId, petId, vetId);
        return id;
    }

    protected UUID seedInvoice(UUID clinicId, UUID ownerId, String invoiceNumber, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO invoice (id, clinic_id, owner_id, invoice_number, status, subtotal, tax_amount, discount_amount, total, currency, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, ?, 0, 0, 0, 0, 'RSD', NOW(), NOW(), false, 0)", id, clinicId, ownerId, invoiceNumber, status);
        return id;
    }

    protected UUID seedInventoryItem(UUID clinicId, String name, String category) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO inventory_item (id, clinic_id, name, category, quantity_on_hand, active, created_at, updated_at, deleted, version) " +
                "VALUES (?, ?, ?, ?, 100, true, NOW(), NOW(), false, 0)", id, clinicId, name, category);
        return id;
    }
}
