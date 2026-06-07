package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryCategory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        InventoryCategory category,
        String unit,
        Boolean trackBatches,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}