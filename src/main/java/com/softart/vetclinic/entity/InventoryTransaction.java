package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.InventoryTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_transaction")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class InventoryTransaction extends BaseEntity {

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryTransactionType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;
    
    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", insertable = false, updatable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", insertable = false, updatable = false)
    private User performedByUser;
}
