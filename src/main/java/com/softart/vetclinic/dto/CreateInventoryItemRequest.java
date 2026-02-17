package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInventoryItemRequest(
        UUID locationId,
        @NotBlank String name,
        String sku,
        @NotNull InventoryCategory category,
        BigDecimal quantityOnHand,
        String unit,
        BigDecimal reorderLevel,
        BigDecimal costPrice,
        BigDecimal sellPrice,
        LocalDate expiryDate,
        Boolean active
) {}
