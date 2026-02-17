package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "email"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class User extends BaseEntity {

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 30)
    private String phone;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(length = 100)
    private String specialization;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;
}
