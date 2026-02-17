package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pet")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Pet extends BaseEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "species_id")
    private UUID speciesId;

    @Column(name = "breed_id")
    private UUID breedId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 50)
    private String color;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "microchip_number", length = 50)
    private String microchipNumber;

    @Column(name = "is_neutered", nullable = false)
    private Boolean isNeutered = false;

    @Column(name = "is_deceased", nullable = false)
    private Boolean isDeceased = false;

    @Column(name = "deceased_at")
    private LocalDate deceasedAt;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", insertable = false, updatable = false)
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", insertable = false, updatable = false)
    private Breed breed;
}
