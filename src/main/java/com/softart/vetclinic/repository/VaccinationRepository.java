package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Vaccination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VaccinationRepository extends JpaRepository<Vaccination, UUID> {

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Vaccination> findByClinicIdAndPetId(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Vaccination> findByClinicIdAndNextDueDateBefore(UUID clinicId, LocalDate date);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Optional<Vaccination> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    Page<Vaccination> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Vaccination> findByClinicIdAndPetIdAndDeletedFalse(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"pet", "vet"})
    List<Vaccination> findByClinicIdAndDeletedFalseAndNextDueDateBefore(UUID clinicId, LocalDate date);
    
    @EntityGraph(attributePaths = {"pet"})
    @Query("SELECT v FROM Vaccination v WHERE v.nextDueDate IS NOT NULL " +
           "AND v.nextDueDate <= :reminderDate AND v.nextDueDate >= CURRENT_DATE " +
           "AND v.deleted = false")
    List<Vaccination> findUpcomingDueVaccinations(@Param("reminderDate") LocalDate reminderDate);

}
