package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryTransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateInventoryTransactionRequest(
        UUID inventoryItemId,
        InventoryTransactionType type,
        BigDecimal quantity,
        String referenceType,
        UUID referenceId,
        UUID performedBy,
        String note
) {}
