package com.softart.vetclinic.config.security;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        UUID clinicId,
        String email,
        String role
) {}
