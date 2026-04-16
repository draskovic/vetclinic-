package com.softart.vetclinic.dto;

import java.util.List;
import java.util.UUID;

public record BookingOwnerLookupResponse(
    boolean found,
    UUID ownerId,
    String ownerName,
    List<PetInfo> pets
) {
    public record PetInfo(UUID id, String name, String speciesName) {}
}
