package com.softart.vetclinic.dto;

public record UpdateSpeciesRequest(
        String name,
        Boolean active
) {}
