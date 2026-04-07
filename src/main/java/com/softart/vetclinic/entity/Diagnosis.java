package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diagnosis", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "name"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Diagnosis extends BaseEntity {

    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;
}
