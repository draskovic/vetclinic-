package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record CreateClinicRequest(
        @NotBlank String name,
        String taxId,
        String email,
        String phone,
        String address,
        String city,
        String country,
        String registrationNumber,
        String activityCode,
        String bankAccount,
        Boolean vatPayer,
        String veterinaryLicenseNumber,
        String logoUrl,
        SubscriptionPlan subscriptionPlan,
        OffsetDateTime subscriptionExpiresAt,
        Boolean active,
        String settings
) {}
