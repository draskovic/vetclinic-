package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InventoryItem extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "sell_price", precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "tax_rate_id", nullable = false)
    private UUID taxRateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", insertable = false, updatable = false)
    private ClinicLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}