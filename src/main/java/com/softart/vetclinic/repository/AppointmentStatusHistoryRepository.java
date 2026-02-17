package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.AppointmentStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, UUID> {

    List<AppointmentStatusHistory> findByAppointmentIdOrderByCreatedAtAsc(UUID appointmentId);

    Optional<AppointmentStatusHistory> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<AppointmentStatusHistory> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<AppointmentStatusHistory> findByClinicIdAndAppointmentIdAndDeletedFalseOrderByCreatedAtAsc(UUID clinicId, UUID appointmentId);
}
