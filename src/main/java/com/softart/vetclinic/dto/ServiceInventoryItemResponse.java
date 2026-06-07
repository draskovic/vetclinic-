package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceInventoryItemResponse(
    UUID id,
    UUID serviceId,
    String serviceName,
    UUID productId,
    String productName,
    BigDecimal quantityPerUse,
    String unit,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}