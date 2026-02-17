package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Service;
import com.softart.vetclinic.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByClinicIdAndActiveTrue(UUID clinicId);

    List<Service> findByClinicIdAndCategoryAndActiveTrue(UUID clinicId, ServiceCategory category);

    Optional<Service> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<Service> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    boolean existsByClinicIdAndNameAndDeletedFalse(UUID clinicId, String name);
}
