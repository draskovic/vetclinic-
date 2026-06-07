package com.softart.vetclinic.repository;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.softart.vetclinic.entity.MedicationAdministration;

@Repository
public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, UUID> {

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem", "inventoryItem.product"})
    Optional<MedicationAdministration> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem", "inventoryItem.product"})
    Page<MedicationAdministration> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem", "inventoryItem.product"})
    List<MedicationAdministration> findByClinicIdAndMedicalRecordIdAndDeletedFalseOrderByAdministeredDateDesc(UUID clinicId, UUID medicalRecordId);

    @EntityGraph(attributePaths = {"pet", "vet", "inventoryItem", "inventoryItem.product"})
    List<MedicationAdministration> findByClinicIdAndPetIdAndDeletedFalseOrderByAdministeredDateDesc(UUID clinicId, UUID petId);
    
    // Dodato za potrebe izbora Aplikovanih lekova iz liste zbog dodavanja vise lekova od jednom.
    interface MedicationQuickPickRow {
        UUID getInventoryItemId();
        String getName();
        BigDecimal getQuantityOnHand();
        String getUnit();
    }

    @Query(value = """
            SELECT i.id AS inventoryItemId, p.name AS name,
                   COALESCE((SELECT SUM(b.quantity_on_hand) FROM inventory_batch b
                             WHERE b.inventory_item_id = i.id AND b.deleted = false), 0) AS quantityOnHand,
                   p.unit AS unit
            FROM (
                SELECT inventory_item_id, MAX(administered_date) AS last_used
                FROM medication_administration
                WHERE clinic_id = :clinicId AND deleted = false AND inventory_item_id IS NOT NULL
                GROUP BY inventory_item_id
            ) recent
            JOIN inventory_item i ON i.id = recent.inventory_item_id AND i.deleted = false
            JOIN product p ON p.id = i.product_id
            ORDER BY recent.last_used DESC
            LIMIT :limit
            """, nativeQuery = true)
        List<MedicationQuickPickRow> findRecentDistinctMedications(
                @Param("clinicId") UUID clinicId,
                @Param("limit") int limit);

        @Query(value = """
            SELECT i.id AS inventoryItemId, p.name AS name,
                   COALESCE((SELECT SUM(b.quantity_on_hand) FROM inventory_batch b
                             WHERE b.inventory_item_id = i.id AND b.deleted = false), 0) AS quantityOnHand,
                   p.unit AS unit
            FROM (
                SELECT inventory_item_id, COUNT(*) AS usage_count
                FROM medication_administration
                WHERE clinic_id = :clinicId AND deleted = false
                      AND inventory_item_id IS NOT NULL
                      AND administered_date >= :since
                GROUP BY inventory_item_id
            ) freq
            JOIN inventory_item i ON i.id = freq.inventory_item_id AND i.deleted = false
            JOIN product p ON p.id = i.product_id
            ORDER BY freq.usage_count DESC, p.name ASC
            LIMIT :limit
            """, nativeQuery = true)
        List<MedicationQuickPickRow> findFrequentMedications(
                @Param("clinicId") UUID clinicId,
                @Param("limit") int limit,
                @Param("since") LocalDate since);
}