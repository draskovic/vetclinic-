package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateInvoiceItemRequest(
        UUID invoiceId,
        UUID serviceId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal discountPercent,
        BigDecimal lineTotal,
        Integer sortOrder
) {}
