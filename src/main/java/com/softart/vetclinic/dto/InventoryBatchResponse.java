package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryBatchResponse(
        UUID id,
        UUID inventoryItemId,
        String inventoryItemName,
        String inventoryItemUnit,
        String batchNumber,
        LocalDate expiryDate,
        BigDecimal quantityOnHand,
        LocalDate receivedAt,
        String supplier,
        BigDecimal costPrice,
        String notes,
        Long daysUntilExpiry,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
