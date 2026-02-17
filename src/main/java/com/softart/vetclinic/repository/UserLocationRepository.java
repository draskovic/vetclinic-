package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.UserLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {

    List<UserLocation> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"location", "user"})
    Optional<UserLocation> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location", "user"})
    Page<UserLocation> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location", "user"})
    List<UserLocation> findByClinicIdAndUserIdAndDeletedFalse(UUID clinicId, UUID userId);
}
