package com.softart.vetclinic.dto;

public record UpdateRoleRequest(
        String name,
        String permissions
) {}
