package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.InventoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank String name,
        String sku,
        @NotNull InventoryCategory category,
        String unit,
        Boolean trackBatches,
        Boolean active
) {}