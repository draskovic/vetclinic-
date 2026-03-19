package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;

public record ProvisionClinicRequest(
    @NotBlank String name,
    String email,
    String phone,
    String address,
    String city,
    String country,
    String taxId,
    @NotBlank String adminFirstName,
    @NotBlank String adminLastName,
    @NotBlank String adminEmail,
    @NotBlank String adminPassword
) {}
