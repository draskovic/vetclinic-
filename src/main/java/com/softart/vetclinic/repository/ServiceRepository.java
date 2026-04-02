package com.softart.vetclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.Service;
import com.softart.vetclinic.enums.ServiceCategory;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByClinicIdAndActiveTrue(UUID clinicId);

    List<Service> findByClinicIdAndCategoryAndActiveTrue(UUID clinicId, ServiceCategory category);

    Optional<Service> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Service> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndNameAndDeletedFalse(UUID clinicId, String name);
    
    Optional<Service> findByClinicIdAndSkuAndDeletedFalse(UUID clinicId, String sku);

    @Query("SELECT s FROM Service s WHERE s.clinicId = :clinicId AND s.deleted = false " +
    	       "AND (:search = '' OR (LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
    	       "OR LOWER(s.sku) LIKE LOWER(CONCAT('%', :search, '%')))) " +
    	       "AND (:category IS NULL OR s.category = :category)")
    	Page<com.softart.vetclinic.entity.Service> searchByClinicIdAndCategory(
    	    @Param("clinicId") UUID clinicId,
    	    @Param("search") String search,
    	    @Param("category") ServiceCategory category,
    	    Pageable pageable);

}
