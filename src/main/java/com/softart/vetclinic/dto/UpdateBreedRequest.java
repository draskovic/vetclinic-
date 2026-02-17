package com.softart.vetclinic.dto;

import java.util.UUID;

public record UpdateBreedRequest(
        UUID speciesId,
        String name
) {}
