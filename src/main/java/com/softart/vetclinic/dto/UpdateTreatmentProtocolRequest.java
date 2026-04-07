package com.softart.vetclinic.dto;

import java.util.UUID;

public record UpdateTreatmentProtocolRequest(
    String name,
    String description,
    UUID diagnosisId,
    Boolean active
) {}
