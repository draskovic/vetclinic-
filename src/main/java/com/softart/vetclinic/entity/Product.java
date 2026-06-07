package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.InventoryCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryCategory category;

    @Column(length = 20)
    private String unit;

    @Column(name = "track_batches", nullable = false)
    private Boolean trackBatches = false;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;
}