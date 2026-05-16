package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicationQuickPickItem(
        UUID inventoryItemId,
        String name,
        BigDecimal quantityOnHand,
        String unit
) {}