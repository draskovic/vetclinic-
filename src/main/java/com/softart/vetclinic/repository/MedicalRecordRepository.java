package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

    List<MedicalRecord> findByClinicIdAndPetIdOrderByCreatedAtDesc(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<MedicalRecord> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Page<MedicalRecord> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<MedicalRecord> findByClinicIdAndPetIdAndDeletedFalseOrderByCreatedAtDesc(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<MedicalRecord> findByAppointmentIdAndDeletedFalse(UUID appointmentId);
}
