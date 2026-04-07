package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTreatmentProtocolItemRequest(
    @NotNull UUID protocolId,
    @NotNull UUID serviceId,
    Integer quantity,
    String notes,
    Integer sortOrder
) {}
