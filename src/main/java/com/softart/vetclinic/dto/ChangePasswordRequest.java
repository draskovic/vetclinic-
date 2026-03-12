package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Trenutna lozinka je obavezna")
    String currentPassword,

    @NotBlank(message = "Nova lozinka je obavezna")
    @Size(min = 6, max = 100, message = "Nova lozinka mora imati najmanje 6 karaktera")
    String newPassword
) {}
