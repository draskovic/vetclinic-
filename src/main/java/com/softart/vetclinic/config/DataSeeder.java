package com.softart.vetclinic.config;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.entity.Role;
import com.softart.vetclinic.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        // Use SECURITY DEFINER function to count users (bypasses RLS)
        Long userCount = ((Number) entityManager
                .createNativeQuery("SELECT count_all_users()")
                .getSingleResult()).longValue();

        if (userCount > 0) {
            log.info("Database already has {} users, skipping seed.", userCount);
            return;
        }

        log.info("Seeding initial test data...");

        // Step 1: Generate clinic ID and set RLS context DIRECTLY on the existing connection
        UUID clinicId = UUID.randomUUID();
        ClinicContextHolder.set(clinicId);

        // Set app.current_clinic_id on the CURRENT connection (already acquired by @Transactional)
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (var stmt = connection.createStatement()) {
                stmt.execute("SET app.current_clinic_id = '" + clinicId + "'");
            }
        });

        // Step 2: Insert clinic via native SQL (avoids Hibernate detached entity issue with pre-set ID)
        entityManager.createNativeQuery(
                "INSERT INTO clinic (id, name, email, phone, city, country, subscription_plan, active, settings, created_at, updated_at, deleted, version) " +
                "VALUES (:id, :name, :email, :phone, :city, :country, 'BASIC', true, '{}'::jsonb, NOW(), NOW(), false, 0)")
                .setParameter("id", clinicId)
                .setParameter("name", "Test Vet Clinic")
                .setParameter("email", "clinic@test.com")
                .setParameter("phone", "+381111234567")
                .setParameter("city", "Beograd")
                .setParameter("country", "Serbia")
                .executeUpdate();
        log.info("Created clinic: Test Vet Clinic (id: {})", clinicId);

        // Step 3: Create roles and user using EntityManager.persist()
        Role adminRole = new Role();
        adminRole.setClinicId(clinicId);
        adminRole.setName("ADMIN");
        adminRole.setPermissions("[\"*\"]");
        adminRole.setDeleted(false);
        entityManager.persist(adminRole);
        entityManager.flush();
        log.info("Created role: {} (id: {})", adminRole.getName(), adminRole.getId());

        Role vetRole = new Role();
        vetRole.setClinicId(clinicId);
        vetRole.setName("VET");
        vetRole.setPermissions("[\"manage_appointments\",\"manage_medical_records\",\"manage_prescriptions\",\"view_inventory\"]");
        vetRole.setDeleted(false);
        entityManager.persist(vetRole);
        entityManager.flush();
        log.info("Created role: {} (id: {})", vetRole.getName(), vetRole.getId());

        User admin = new User();
        admin.setClinicId(clinicId);
        admin.setRoleId(adminRole.getId());
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setActive(true);
        admin.setDeleted(false);
        entityManager.persist(admin);
        entityManager.flush();
        log.info("Created user: {} {} (email: {}, id: {})", admin.getFirstName(), admin.getLastName(), admin.getEmail(), admin.getId());

        log.info("Seed data complete! Login with: clinicId={}, email=admin@test.com, password=admin123", clinicId);

        ClinicContextHolder.clear();
    }
}
