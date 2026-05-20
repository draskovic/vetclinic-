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
    
    @PrePersist
    @PreUpdate
    private void trimStringFields() {
        if (firstName != null) firstName = firstName.trim();
        if (lastName != null) lastName = lastName.trim();
        if (email != null) email = email.trim();
        if (phone != null) phone = phone.trim();
        if (address != null) address = address.trim();
        if (city != null) city = city.trim();
        if (personalId != null) personalId = personalId.trim();
        if (clientCode != null) clientCode = clientCode.trim();
        if (note != null) note = note.trim();
    }
}
