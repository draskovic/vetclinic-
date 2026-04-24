package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.SubscriptionPlan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClinicResponse(
        UUID id,
        String name,
        String taxId,
        String email,
        String phone,
        String address,
        String city,
        String country,
        String phoneCountryCode,
        String registrationNumber,
        String activityCode,
        String bankAccount,
        Boolean vatPayer,
        String veterinaryLicenseNumber,
        String logoUrl,
        SubscriptionPlan subscriptionPlan,
        OffsetDateTime subscriptionExpiresAt,
        Boolean active,
        String settings,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
