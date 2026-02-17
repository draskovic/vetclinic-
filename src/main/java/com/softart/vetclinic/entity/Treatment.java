package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "treatment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Treatment extends BaseEntity {

    @Column(name = "medical_record_id", nullable = false)
    private UUID medicalRecordId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tooth_chart", columnDefinition = "JSONB")
    private String toothChart;

    @Column(columnDefinition = "TEXT")
    private String result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", insertable = false, updatable = false)
    private User vet;
}
