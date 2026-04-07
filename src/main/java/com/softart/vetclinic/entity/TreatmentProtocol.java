package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "treatment_protocol", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "name"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TreatmentProtocol extends BaseEntity {

    @Column(name = "diagnosis_id")
    private java.util.UUID diagnosisId;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", insertable = false, updatable = false)
    private Diagnosis diagnosis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;
}
