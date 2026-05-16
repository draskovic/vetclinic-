package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateInventoryItemRequest(
        UUID locationId,
        String name,
        String sku,
        InventoryCategory category,
        BigDecimal quantityOnHand,
        String unit,
        BigDecimal reorderLevel,
        BigDecimal costPrice,
        BigDecimal sellPrice,
        LocalDate expiryDate,
        Boolean active,
        Boolean trackBatches,
        UUID taxRateId
) {}
