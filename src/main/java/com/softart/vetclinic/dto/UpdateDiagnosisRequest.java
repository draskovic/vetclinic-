package com.softart.vetclinic.dto;

public record UpdateDiagnosisRequest(
    String code,
    String name,
    String category,
    String description,
    Boolean active
) {}
