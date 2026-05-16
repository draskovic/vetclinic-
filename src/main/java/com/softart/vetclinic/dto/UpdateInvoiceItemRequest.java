package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateInvoiceItemRequest(
        UUID invoiceId,
        UUID serviceId,
        UUID inventoryItemId,
        UUID treatmentId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        UUID taxRateId,
        BigDecimal discountPercent,
        BigDecimal lineTotal,
        Integer sortOrder
) {}