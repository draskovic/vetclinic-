package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.enums.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    @EntityGraph(attributePaths = {"location"})
    List<InventoryItem> findByClinicIdAndCategoryAndActiveTrue(UUID clinicId, InventoryCategory category);

    @EntityGraph(attributePaths = {"location"})
    Optional<InventoryItem> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location"})
    Page<InventoryItem> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);
    
    @EntityGraph(attributePaths = {"location"})
    @Query("SELECT i FROM InventoryItem i WHERE i.clinicId = :clinicId AND i.deleted = false " +
           "AND (:category IS NULL OR i.category = :category) " +
           "AND (:search = '' OR (LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.unit) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<InventoryItem> searchByClinicIdAndCategory(
            @Param("clinicId") UUID clinicId,
            @Param("search") String search,
            @Param("category") InventoryCategory category,
            Pageable pageable);


}
