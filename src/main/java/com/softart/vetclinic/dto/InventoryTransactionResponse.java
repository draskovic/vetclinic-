package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryTransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.softart.vetclinic.enums.AdjustmentReason;

public record InventoryTransactionResponse(
        UUID id,
        UUID inventoryItemId,
        String inventoryItemName,
        UUID batchId,
        InventoryTransactionType type,
        BigDecimal quantity,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String performedByName,
        String note,
        AdjustmentReason reason,
        UUID reversalOfTransactionId,      // ← NOVO
        boolean reversed,                   // ← NOVO
        String batchNumber,
        boolean batchDeleted,               // ← NOVO (popunjava kontroler)
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
