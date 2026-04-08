package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_record_diagnosis")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MedicalRecordDiagnosis {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "medical_record_id", nullable = false)
    private UUID medicalRecordId;

    @Column(name = "diagnosis_id", nullable = false)
    private UUID diagnosisId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", insertable = false, updatable = false)
    private Diagnosis diagnosis;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
