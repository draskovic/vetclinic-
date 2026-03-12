package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Ime je obavezno")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Prezime je obavezno")
    @Size(max = 100)
    String lastName,

    @Size(max = 50)
    String phone,

    @Size(max = 50)
    String licenseNumber,

    @Size(max = 200)
    String specialization
) {}
