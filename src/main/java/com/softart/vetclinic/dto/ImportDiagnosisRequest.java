package com.softart.vetclinic.dto;

public record ImportDiagnosisRequest(
        String name,
        String code,
        String category,
        String description
) {}
