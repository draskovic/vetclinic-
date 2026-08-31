package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_record")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MedicalRecord extends BaseEntity {

    @Column(name = "appointment_id", unique = true)
    private UUID appointmentId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;
    
    @Column(name = "location_id")
    private UUID locationId;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "record_code", length = 20)
    private String recordCode;

    @Column(name = "examination_notes", columnDefinition = "TEXT")
    private String examinationNotes;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "temperature_c", precision = 4, scale = 1)
    private BigDecimal temperatureC;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "follow_up_recommended", nullable = false)
    private Boolean followUpRecommended = false;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", insertable = false, updatable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", insertable = false, updatable = false)
    private User vet;
}
