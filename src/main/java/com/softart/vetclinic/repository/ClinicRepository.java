package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    @Query(value = "SELECT * FROM get_clinic_by_email(:email)", nativeQuery = true)
    Optional<Clinic> findByEmailBypassRls(@Param("email") String email);
}