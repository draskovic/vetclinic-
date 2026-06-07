package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceInventoryItemRequest(
    @NotNull UUID serviceId,
    @NotNull UUID productId,
    BigDecimal quantityPerUse
) {}