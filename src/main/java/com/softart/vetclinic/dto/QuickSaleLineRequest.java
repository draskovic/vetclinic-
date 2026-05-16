package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Jedna stavka Quick Sale fakture.
 *
 * Mora imati serviceId ILI inventoryItemId (DB CHECK constraint chk_invoice_item_source).
 * Servis validira tu invarijantu pre poziva DB-a i baca jasan 400.
 *
 * Server-side resolve pravila (kad klijent prosledi null):
 *  - unitPrice       → service.price ili inventoryItem.sellPrice
 *  - taxRateId       → service.taxRateId ili inventoryItem.taxRateId (snapshot iz šifarnika)
 *  - description     → service.name ili inventoryItem.name
 *  - discountPercent → 0
 */
public record QuickSaleLineRequest(
        UUID serviceId,
        UUID inventoryItemId,
        String description,
        @NotNull @Positive BigDecimal quantity,
        BigDecimal unitPrice,
        UUID taxRateId,
        BigDecimal discountPercent
) {}