package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "species", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "name"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Species extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;
}
