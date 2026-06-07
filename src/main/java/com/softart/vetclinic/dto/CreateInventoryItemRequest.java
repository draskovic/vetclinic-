package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInventoryItemRequest(
        @NotNull UUID productId,
        UUID locationId,
        BigDecimal reorderLevel,
        BigDecimal sellPrice,
        Boolean active,
        BigDecimal initialQuantity,
        @NotNull UUID taxRateId
) {}