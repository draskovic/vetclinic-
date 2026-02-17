package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryTransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryTransactionResponse(
        UUID id,
        UUID inventoryItemId,
        String inventoryItemName,
        InventoryTransactionType type,
        BigDecimal quantity,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String performedByName,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
