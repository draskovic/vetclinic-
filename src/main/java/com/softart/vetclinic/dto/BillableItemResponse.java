package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.BillableItemType;

import java.math.BigDecimal;
import java.util.UUID;

public record BillableItemResponse(
        BillableItemType type,
        UUID id,
        String name,
        String sku,
        String unit,
        BigDecimal unitPrice,
        UUID taxRateId,
        String taxRateLabel,
        BigDecimal taxRatePercent,
        BigDecimal quantityOnHand, // null za SERVICE
        Boolean trackBatches       // null za SERVICE
) {}