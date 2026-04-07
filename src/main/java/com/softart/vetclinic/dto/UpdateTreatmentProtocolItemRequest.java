package com.softart.vetclinic.dto;

public record UpdateTreatmentProtocolItemRequest(
    Integer quantity,
    String notes,
    Integer sortOrder
) {}
