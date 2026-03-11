package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    List<Appointment> findByClinicIdAndStartTimeBetween(UUID clinicId, OffsetDateTime from, OffsetDateTime to);

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    List<Appointment> findByClinicIdAndVetIdAndStartTimeBetween(UUID clinicId, UUID vetId, OffsetDateTime from, OffsetDateTime to);

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    Optional<Appointment> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    Page<Appointment> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    List<Appointment> findByClinicIdAndDeletedFalseAndStartTimeBetween(UUID clinicId, OffsetDateTime from, OffsetDateTime to);

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    List<Appointment> findByClinicIdAndDeletedFalseAndVetIdAndStartTimeBetween(UUID clinicId, UUID vetId, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.clinicId = :clinicId AND a.vetId = :vetId " +
           "AND a.deleted = false AND a.status NOT IN (com.softart.vetclinic.enums.AppointmentStatus.CANCELLED, com.softart.vetclinic.enums.AppointmentStatus.NO_SHOW) " +
           "AND a.startTime < :endTime AND a.endTime > :startTime AND (:excludeId IS NULL OR a.id <> :excludeId)")
    boolean hasOverlappingAppointment(@Param("clinicId") UUID clinicId,
                                      @Param("vetId") UUID vetId,
                                      @Param("startTime") OffsetDateTime startTime,
                                      @Param("endTime") OffsetDateTime endTime,
                                      @Param("excludeId") UUID excludeId);
    
    @Query("SELECT a FROM Appointment a WHERE a.startTime >= :start AND a.startTime < :end " +
    	       "AND a.status IN ('SCHEDULED', 'CONFIRMED') AND a.deleted = false")
    	List<Appointment> findTomorrowAppointments(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
    
    List<Appointment> findByClinicIdAndPetIdAndDeletedFalseOrderByStartTimeDesc(UUID clinicId, UUID petId);

    @EntityGraph(attributePaths = {"location", "pet", "owner", "vet"})
    @Query("SELECT a FROM Appointment a WHERE a.clinicId = :clinicId AND a.deleted = false " +
           "AND (:search IS NULL OR LOWER(a.pet.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.owner.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.owner.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.vet.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.vet.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.reason) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Appointment> searchByClinicId(@Param("clinicId") UUID clinicId, @Param("search") String search, Pageable pageable);
    
    List<Appointment> findByClinicIdAndOwnerIdAndDeletedFalseOrderByStartTimeDesc(UUID clinicId, UUID ownerId);


}
