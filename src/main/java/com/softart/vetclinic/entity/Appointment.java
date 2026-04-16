package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.enums.AppointmentType;
import com.softart.vetclinic.enums.BookingSource;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Appointment extends BaseEntity {

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentType type;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_to")
    private UUID followUpTo;
    
    @Column(name = "cancellation_token", length = 100, unique = true)
    private String cancellationToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", nullable = false, length = 20)
    private BookingSource bookingSource = BookingSource.CLINIC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", insertable = false, updatable = false)
    private ClinicLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", insertable = false, updatable = false)
    private User vet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_to", insertable = false, updatable = false)
    private Appointment followUpAppointment;
}
