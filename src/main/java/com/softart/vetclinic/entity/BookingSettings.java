package com.softart.vetclinic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_settings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BookingSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clinic_id", nullable = false, unique = true)
    private UUID clinicId;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 30;

    @Column(name = "buffer_minutes", nullable = false)
    private Integer bufferMinutes = 0;

    @Column(name = "max_advance_days", nullable = false)
    private Integer maxAdvanceDays = 30;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_types", columnDefinition = "JSONB", nullable = false)
    private String allowedTypes = "[\"CHECKUP\",\"VACCINATION\",\"GROOMING\"]";

    @Column(name = "auto_confirm", nullable = false)
    private Boolean autoConfirm = false;

    @Column(name = "allow_vet_selection", nullable = false)
    private Boolean allowVetSelection = false;

    @Column(name = "cancellation_hours", nullable = false)
    private Integer cancellationHours = 24;
    
    @Column(nullable = false, length = 50)
    private String timezone = "Europe/Belgrade";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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
