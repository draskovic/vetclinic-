package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.BookingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingSettingsRepository extends JpaRepository<BookingSettings, UUID> {
    Optional<BookingSettings> findByClinicId(UUID clinicId);
}
