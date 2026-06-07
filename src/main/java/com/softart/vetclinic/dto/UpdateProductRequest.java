package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryCategory;

public record UpdateProductRequest(
        String name,
        String sku,
        InventoryCategory category,
        String unit,
        Boolean trackBatches,
        Boolean active
) {}