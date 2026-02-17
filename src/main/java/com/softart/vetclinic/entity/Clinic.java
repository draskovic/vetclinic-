package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinic")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Clinic {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "tax_id", length = 50, unique = true)
    private String taxId;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country = "Serbia";

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 30)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BASIC;

    @Column(name = "subscription_expires_at")
    private OffsetDateTime subscriptionExpiresAt;

    @Column(nullable = false)
    private Boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    private String settings = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
