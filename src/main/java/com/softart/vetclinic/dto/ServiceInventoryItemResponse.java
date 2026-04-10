package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceInventoryItemResponse(
    UUID id,
    UUID serviceId,
    String serviceName,
    UUID inventoryItemId,
    String inventoryItemName,
    BigDecimal quantityPerUse,
    String unit,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
