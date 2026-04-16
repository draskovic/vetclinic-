package com.softart.vetclinic.dto;

import java.util.List;
import java.util.UUID;

public record BookingClinicInfoResponse(
    String clinicName,
    String clinicPhone,
    String clinicAddress,
    String clinicCity,
    String logoUrl,
    List<LocationInfo> locations,
    List<String> allowedTypes,
    Integer slotDurationMinutes,
    Integer maxAdvanceDays,
    Boolean allowVetSelection,
    List<VetInfo> vets
) {
    public record LocationInfo(UUID id, String name, String address) {}
    public record VetInfo(UUID id, String name, String specialization) {}
}
