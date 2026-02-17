package com.softart.vetclinic.dto;

public record UpdateClinicLocationRequest(
        String name,
        String address,
        String city,
        String phone,
        String email,
        Boolean isMain,
        Boolean active,
        String workingHours
) {}
