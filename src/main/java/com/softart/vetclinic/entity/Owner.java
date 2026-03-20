package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "owner")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Owner extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(name = "personal_id", length = 30)
    private String personalId;

    @Column(columnDefinition = "TEXT")
    private String note;
    
    @Column(name = "client_code", length = 20)
    private String clientCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;
}
