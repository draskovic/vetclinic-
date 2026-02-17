package com.softart.vetclinic.dto;

public record UpdateOwnerRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String city,
        String personalId,
        String note
) {}
