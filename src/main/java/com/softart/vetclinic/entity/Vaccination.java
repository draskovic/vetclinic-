package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaccination")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Vaccination extends BaseEntity {

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "medical_record_id")
    private UUID medicalRecordId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;

    @Column(name = "vaccine_name", nullable = false, length = 200)
    private String vaccineName;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(length = 200)
    private String manufacturer;

    @Column(name = "administered_at", nullable = false)
    private OffsetDateTime administeredAt;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", insertable = false, updatable = false)
    private User vet;
}
