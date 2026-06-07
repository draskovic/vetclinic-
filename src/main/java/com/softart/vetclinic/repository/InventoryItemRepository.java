package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.enums.InventoryCategory;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    @Query("SELECT i FROM InventoryItem i JOIN i.product p " +
            "WHERE i.clinicId = :clinicId AND i.deleted = false " +
            "AND p.category = :category AND i.active = true")
     List<InventoryItem> findByClinicIdAndProductCategoryAndActiveTrue(
             @Param("clinicId") UUID clinicId,
             @Param("category") InventoryCategory category);

    @EntityGraph(attributePaths = {"location"})
    Optional<InventoryItem> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<InventoryItem> findByClinicIdAndProductIdAndDeletedFalse(UUID clinicId, UUID productId);
    
    @EntityGraph(attributePaths = {"location"})
    Page<InventoryItem> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);
    
    @Query("SELECT i FROM InventoryItem i JOIN i.product p " +
            "WHERE i.clinicId = :clinicId AND i.deleted = false " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:search = '' OR (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.unit) LIKE LOWER(CONCAT('%', :search, '%'))))")
     Page<InventoryItem> searchByClinicIdAndCategory(
             @Param("clinicId") UUID clinicId,
             @Param("search") String search,
             @Param("category") InventoryCategory category,
             Pageable pageable);


    @Query("SELECT i FROM InventoryItem i WHERE i.clinicId = :clinicId AND i.deleted = false " +
            "AND i.active = true AND i.reorderLevel IS NOT NULL " +
            "AND (SELECT COALESCE(SUM(b.quantityOnHand), 0) FROM InventoryBatch b " +
            "     WHERE b.inventoryItemId = i.id AND b.deleted = false) <= i.reorderLevel")
     List<InventoryItem> findLowStock(@Param("clinicId") UUID clinicId);

     @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.clinicId = :clinicId AND i.deleted = false " +
            "AND i.active = true AND i.reorderLevel IS NOT NULL " +
            "AND (SELECT COALESCE(SUM(b.quantityOnHand), 0) FROM InventoryBatch b " +
            "     WHERE b.inventoryItemId = i.id AND b.deleted = false) <= i.reorderLevel")
     long countLowStock(@Param("clinicId") UUID clinicId);
     
     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT i FROM InventoryItem i WHERE i.id = :id AND i.clinicId = :clinicId AND i.deleted = false")
     Optional<InventoryItem> findByIdAndClinicIdAndDeletedFalseForUpdate(@Param("id") UUID id, @Param("clinicId") UUID clinicId);

     @Query("SELECT i.id FROM InventoryItem i WHERE i.id IN :ids " +
             "AND i.clinicId = :clinicId AND i.deleted = false")
      List<UUID> findExistingIdsByIdsAndClinicId(
              @Param("ids") List<UUID> ids,
              @Param("clinicId") UUID clinicId);
}
