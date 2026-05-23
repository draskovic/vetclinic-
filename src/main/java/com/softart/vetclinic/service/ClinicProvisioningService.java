package com.softart.vetclinic.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.softart.vetclinic.dto.ProvisionClinicRequest;
import com.softart.vetclinic.dto.ProvisionClinicResponse;
import com.softart.vetclinic.entity.ClinicLocation;
import com.softart.vetclinic.entity.Role;
import com.softart.vetclinic.entity.User;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicProvisioningService {

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ProvisionClinicResponse provision(UUID clinicId, ProvisionClinicRequest request) {
        // Insert clinic via native SQL
        entityManager.createNativeQuery(
                "INSERT INTO clinic (id, name, email, phone, address, city, country, tax_id, subscription_plan, active, settings, created_at, updated_at, deleted, version) " +
                "VALUES (:id, :name, :email, :phone, :address, :city, :country, :taxId, 'BASIC', true, '{}'::jsonb, NOW(), NOW(), false, 0)")
                .setParameter("id", clinicId)
                .setParameter("name", request.name())
                .setParameter("email", request.email())
                .setParameter("phone", request.phone())
                .setParameter("address", request.address())
                .setParameter("city", request.city())
                .setParameter("country", request.country())
                .setParameter("taxId", request.taxId())
                .executeUpdate();
        log.info("Provisioned clinic: {} (id: {})", request.name(), clinicId);

        // Create ADMIN role
        Role adminRole = new Role();
        adminRole.setClinicId(clinicId);
        adminRole.setName("ADMIN");
        adminRole.setPermissions("[\"*\"]");
        adminRole.setDeleted(false);
        entityManager.persist(adminRole);
        entityManager.flush();

        // Create VET role
        Role vetRole = new Role();
        vetRole.setClinicId(clinicId);
        vetRole.setName("VET");
        vetRole.setPermissions("[\"manage_appointments\",\"view_appointments\",\"manage_medical_records\",\"view_medical_records\",\"manage_pets\",\"view_pets\",\"view_owners\",\"manage_vaccinations\",\"view_vaccinations\",\"manage_lab_reports\",\"view_lab_reports\",\"manage_prescriptions\",\"view_prescriptions\",\"manage_inventory\",\"view_inventory\",\"manage_documents\",\"view_documents\",\"view_invoices\"]");
        vetRole.setDeleted(false);
        entityManager.persist(vetRole);
        entityManager.flush();

        // Create admin user
        User admin = new User();
        admin.setClinicId(clinicId);
        admin.setRoleId(adminRole.getId());
        admin.setFirstName(request.adminFirstName());
        admin.setLastName(request.adminLastName());
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setActive(true);
        admin.setDeleted(false);
        entityManager.persist(admin);
        entityManager.flush();
        log.info("Provisioned admin user: {} {} (email: {})", request.adminFirstName(), request.adminLastName(), request.adminEmail());

        // Create default location
        ClinicLocation defaultLocation = new ClinicLocation();
        defaultLocation.setClinicId(clinicId);
        defaultLocation.setName(request.name());
        defaultLocation.setAddress(request.address());
        defaultLocation.setCity(request.city());
        defaultLocation.setPhone(request.phone());
        defaultLocation.setEmail(request.email());
        defaultLocation.setIsMain(true);
        defaultLocation.setActive(true);
        defaultLocation.setDeleted(false);
        entityManager.persist(defaultLocation);
        entityManager.flush();
        log.info("Provisioned default location for clinic: {}", clinicId);

        return new ProvisionClinicResponse(clinicId, request.name(), request.adminEmail(), request.adminPassword());
    
    }
}
