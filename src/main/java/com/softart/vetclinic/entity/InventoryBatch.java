package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_batch")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InventoryBatch extends BaseEntity {

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity_on_hand", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Column(length = 255)
    private String supplier;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", insertable = false, updatable = false)
    private InventoryItem inventoryItem;
}
