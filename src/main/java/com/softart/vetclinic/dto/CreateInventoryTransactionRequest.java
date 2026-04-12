package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.enums.AdjustmentReason;

import jakarta.validation.constraints.NotNull;

public record CreateInventoryTransactionRequest(
        @NotNull UUID inventoryItemId,
        @NotNull InventoryTransactionType type,
        @NotNull BigDecimal quantity,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String note,
        AdjustmentReason reason
) {}

