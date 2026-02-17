package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInventoryTransactionRequest(
        @NotNull UUID inventoryItemId,
        @NotNull InventoryTransactionType type,
        @NotNull BigDecimal quantity,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String note
) {}
