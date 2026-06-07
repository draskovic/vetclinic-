package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Product;
import com.softart.vetclinic.enums.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Product> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @Query("SELECT p FROM Product p WHERE p.clinicId = :clinicId AND p.deleted = false " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:search = '' OR (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.unit) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Product> searchByClinicIdAndCategory(
            @Param("clinicId") UUID clinicId,
            @Param("search") String search,
            @Param("category") InventoryCategory category,
            Pageable pageable);
}