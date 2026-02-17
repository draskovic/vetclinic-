package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"clinic_id", "name"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Role extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    private String permissions = "[]";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;
}
