package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "breed", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"species_id", "name"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Breed extends BaseEntity {

    @Column(name = "species_id", nullable = false)
    private UUID speciesId;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", insertable = false, updatable = false)
    private Species species;
}
