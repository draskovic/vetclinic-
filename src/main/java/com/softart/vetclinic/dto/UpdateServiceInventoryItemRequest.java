package com.softart.vetclinic.dto;

import java.math.BigDecimal;

public record UpdateServiceInventoryItemRequest(
    BigDecimal quantityPerUse
) {}
