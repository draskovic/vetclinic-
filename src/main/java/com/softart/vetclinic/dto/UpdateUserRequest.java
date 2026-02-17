package com.softart.vetclinic.dto;

import java.util.UUID;

public record UpdateUserRequest(
        UUID roleId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String licenseNumber,
        String specialization,
        Boolean active
) {}
