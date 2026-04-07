package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TreatmentProtocolItemResponse(
    UUID id,
    UUID protocolId,
    UUID serviceId,
    String serviceName,
    String serviceSku,
    BigDecimal servicePrice,
    Integer quantity,
    String notes,
    Integer sortOrder,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
