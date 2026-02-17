package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "prescription")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Prescription extends BaseEntity {

    @Column(name = "medical_record_id", nullable = false)
    private UUID medicalRecordId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(nullable = false, length = 100)
    private String frequency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", insertable = false, updatable = false)
    private User vet;
}
