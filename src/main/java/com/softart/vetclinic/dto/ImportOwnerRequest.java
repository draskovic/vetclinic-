package com.softart.vetclinic.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ImportOwnerRequest(
    String clientCode,
    @NotBlank String firstName,
    String lastName,
    String phone,
    String address,
    String city,
    List<ImportPetData> pets
) {}
