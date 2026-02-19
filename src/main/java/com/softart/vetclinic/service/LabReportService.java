package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.LabReport;
import com.softart.vetclinic.enums.LabReportStatus;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.LabReportRepository;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LabReportService extends AbstractCrudService<LabReport, LabReportRepository> {

    private final LabReportRepository labReportRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final FileStorageService fileStorageService;

    public LabReportService(LabReportRepository labReportRepository,
                            PetRepository petRepository,
                            UserRepository userRepository,
                            MedicalRecordRepository medicalRecordRepository,
                            FileStorageService fileStorageService) {
        super(labReportRepository);
        this.labReportRepository = labReportRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    protected String getEntityName() {
        return "LabReport";
    }

    @Override
    protected Optional<LabReport> findByIdAndClinicId(UUID id, UUID clinicId) {
        return labReportRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<LabReport> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return labReportRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return labReportRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(LabReport entity) {
        if (labReportRepository.existsByClinicIdAndReportNumberAndDeletedFalse(entity.getClinicId(), entity.getReportNumber())) {
            throw new DuplicateResourceException("LabReport", "reportNumber", entity.getReportNumber());
        }

        requireExists(
                petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                "Pet", "id", entity.getPetId()
        );
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getVetId(), entity.getClinicId()),
                "User", "id", entity.getVetId()
        );
        if (entity.getMedicalRecordId() != null) {
            requireExists(
                    medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getMedicalRecordId(), entity.getClinicId()),
                    "MedicalRecord", "id", entity.getMedicalRecordId()
            );
        }
    }

    // ========================================================================
    // File operations
    // ========================================================================

    @Transactional
    public LabReport uploadFile(UUID id, UUID clinicId, MultipartFile file) {
        LabReport labReport = findById(id, clinicId);

        // Ako već postoji fajl, obriši ga
        if (labReport.getStoragePath() != null) {
            fileStorageService.delete(labReport.getStoragePath());
        }

        String subPath = "lab-reports/" + clinicId;
        String storagePath = fileStorageService.save(file, subPath);

        labReport.setFileName(file.getOriginalFilename());
        labReport.setMimeType(file.getContentType());
        labReport.setFileSizeBytes(file.getSize());
        labReport.setStoragePath(storagePath);

        return labReportRepository.save(labReport);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(UUID id, UUID clinicId) {
        LabReport labReport = findById(id, clinicId);

        if (labReport.getStoragePath() == null) {
            throw new ResourceNotFoundException("File", "labReportId", id);
        }

        return fileStorageService.load(labReport.getStoragePath());
    }

    @Transactional
    public LabReport deleteFile(UUID id, UUID clinicId) {
        LabReport labReport = findById(id, clinicId);

        if (labReport.getStoragePath() != null) {
            fileStorageService.delete(labReport.getStoragePath());
        }

        labReport.setFileName(null);
        labReport.setMimeType(null);
        labReport.setFileSizeBytes(null);
        labReport.setStoragePath(null);

        return labReportRepository.save(labReport);
    }

    @Override
    @Transactional
    public void softDelete(UUID id, UUID clinicId) {
        LabReport entity = findById(id, clinicId);
        // Obriši fajl sa diska pre soft delete-a
        if (entity.getStoragePath() != null) {
            fileStorageService.delete(entity.getStoragePath());
        }
        entity.setDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    // ========================================================================
    // Query methods
    // ========================================================================

    @Transactional(readOnly = true)
    public List<LabReport> findByPet(UUID clinicId, UUID petId) {
        return labReportRepository.findByClinicIdAndPetIdAndDeletedFalse(clinicId, petId);
    }

    @Transactional(readOnly = true)
    public List<LabReport> findByStatus(UUID clinicId, LabReportStatus status) {
        return labReportRepository.findByClinicIdAndStatusAndDeletedFalse(clinicId, status);
    }

    @Transactional(readOnly = true)
    public List<LabReport> findByVet(UUID clinicId, UUID vetId) {
        return labReportRepository.findByClinicIdAndVetIdAndDeletedFalse(clinicId, vetId);
    }
    
    @Transactional(readOnly = true)
    public List<LabReport> findByMedicalRecord(UUID clinicId, UUID medicalRecordId) {
        return labReportRepository.findByClinicIdAndMedicalRecordIdAndDeletedFalse(clinicId, medicalRecordId);
    }

}
