package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplyProtocolRequest(
    @NotNull UUID protocolId,
    @NotNull UUID medicalRecordId,
    @NotNull UUID vetId
) {}
