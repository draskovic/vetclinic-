package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClinicLocationRequest(
        @NotBlank String name,
        String address,
        String city,
        String phone,
        String email,
        Boolean isMain,
        Boolean active,
        String workingHours
) {}
