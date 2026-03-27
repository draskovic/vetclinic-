package com.softart.vetclinic.dto;

public record ImportPetData(
    String name,
    String species,
    String breed,
    String legacyCode
) {}
