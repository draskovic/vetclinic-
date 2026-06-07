package com.softart.vetclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.enums.InventoryTransactionType;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findByInventoryItemId(UUID inventoryItemId);

    @EntityGraph(attributePaths = {"inventoryItem", "performedByUser"})
    Optional<InventoryTransaction> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"inventoryItem", "performedByUser"})
    Page<InventoryTransaction> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"inventoryItem", "performedByUser"})
    List<InventoryTransaction> findByClinicIdAndInventoryItemIdAndDeletedFalseOrderByCreatedAtDesc(UUID clinicId, UUID inventoryItemId);
    
    List<InventoryTransaction> findByClinicIdAndReferenceTypeAndReferenceIdAndDeletedFalse(
            UUID clinicId, String referenceType, UUID referenceId);
    
    @Query("SELECT t FROM InventoryTransaction t LEFT JOIN t.inventoryItem i LEFT JOIN i.product p " +
            "WHERE t.clinicId = :clinicId AND t.deleted = false " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:inventoryItemId IS NULL OR t.inventoryItemId = :inventoryItemId) " +
            "AND (:search = '' OR (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(t.note) LIKE LOWER(CONCAT('%', :search, '%'))))")
     Page<InventoryTransaction> searchAll(@Param("clinicId") UUID clinicId,
                                          @Param("search") String search,
                                          @Param("type") InventoryTransactionType type,
                                          @Param("inventoryItemId") UUID inventoryItemId,
                                          Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM InventoryTransaction t WHERE t.id = :id AND t.clinicId = :clinicId AND t.deleted = false")
    Optional<InventoryTransaction> findByIdAndClinicIdAndDeletedFalseForUpdate(
            @Param("id") UUID id,
            @Param("clinicId") UUID clinicId);
    
}
