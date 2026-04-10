package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.ServiceInventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceInventoryItemRepository extends JpaRepository<ServiceInventoryItem, UUID> {

    Optional<ServiceInventoryItem> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<ServiceInventoryItem> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<ServiceInventoryItem> findByClinicIdAndServiceIdAndDeletedFalse(UUID clinicId, UUID serviceId);

    List<ServiceInventoryItem> findByClinicIdAndInventoryItemIdAndDeletedFalse(UUID clinicId, UUID inventoryItemId);
}
