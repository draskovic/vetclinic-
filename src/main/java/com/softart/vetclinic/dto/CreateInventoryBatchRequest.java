package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInventoryBatchRequest(
        @NotNull UUID inventoryItemId,
        @NotBlank String batchNumber,
        LocalDate expiryDate,
        @NotNull BigDecimal quantityOnHand,
        @NotNull LocalDate receivedAt,
        String supplier,
        BigDecimal costPrice,
        String notes
) {}
