package com.softart.vetclinic.dto;

import java.util.UUID;

public record ProvisionClinicResponse(
    UUID clinicId,
    String clinicName,
    String adminEmail,
    String adminPassword
) {}
