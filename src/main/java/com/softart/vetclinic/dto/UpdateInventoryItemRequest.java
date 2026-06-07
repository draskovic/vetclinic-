package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateInventoryItemRequest(
        UUID locationId,
        BigDecimal reorderLevel,
        BigDecimal sellPrice,
        Boolean active,
        UUID taxRateId
) {}