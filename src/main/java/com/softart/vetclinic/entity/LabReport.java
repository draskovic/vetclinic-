package com.softart.vetclinic.entity;

import com.softart.vetclinic.enums.LabReportStatus;
import com.softart.vetclinic.enums.TestCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lab_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LabReport extends BaseEntity implements FileAttachable {

    @Column(name = "report_number", nullable = false, length = 50)
    private String reportNumber;

    @Column(name = "analysis_type", nullable = false, length = 200)
    private String analysisType;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;

    @Column(name = "medical_record_id")
    private UUID medicalRecordId;

    @Column(name = "laboratory_name", length = 200)
    private String laboratoryName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabReportStatus status = LabReportStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "test_category", nullable = false, length = 20)
    private TestCategory testCategory = TestCategory.LABORATORY;


    @Column(name = "requested_at", nullable = false)
    private LocalDate requestedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "is_abnormal", nullable = false)
    private Boolean isAbnormal = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "file_name", length = 300)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    // --- Relationships (read-only, for JOIN fetching) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", insertable = false, updatable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", insertable = false, updatable = false)
    private User vet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", insertable = false, updatable = false)
    private MedicalRecord medicalRecord;
}
