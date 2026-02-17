package com.softart.vetclinic.dto;

import java.util.UUID;

public record UpdateUserLocationRequest(
        UUID userId,
        UUID locationId,
        Boolean isPrimary
) {}
