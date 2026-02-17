package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findByInventoryItemId(UUID inventoryItemId);

    @EntityGraph(attributePaths = {"inventoryItem", "performedByUser"})
    Optional<InventoryTransaction> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"inventoryItem", "performedByUser"})
    Page<InventoryTransaction> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"inventoryItem", "performedByUser"})
    List<InventoryTransaction> findByClinicIdAndInventoryItemIdAndDeletedFalse(UUID clinicId, UUID inventoryItemId);
}
