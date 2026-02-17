package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.enums.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
