package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOwnerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        @NotBlank String phone,
        String address,
        String city,
        String personalId,
        String note
) {}
