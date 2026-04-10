package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInventoryBatchRequest(
        String batchNumber,
        LocalDate expiryDate,
        LocalDate receivedAt,
        String supplier,
        BigDecimal costPrice,
        String notes
) {}
