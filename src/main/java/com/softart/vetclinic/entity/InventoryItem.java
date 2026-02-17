package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.InventoryCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InventoryItem extends BaseEntity {

    @Column(name = "location_id")
    private UUID locationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryCategory category;

    @Column(name = "quantity_on_hand", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "sell_price", precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", insertable = false, updatable = false)
    private ClinicLocation location;
}
