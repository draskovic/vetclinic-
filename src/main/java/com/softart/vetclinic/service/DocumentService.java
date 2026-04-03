package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Document;
import com.softart.vetclinic.config.security.JwtService;
import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.enums.FileType;
import io.jsonwebtoken.Claims;

import java.util.HashMap;
import java.util.Map;

import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.DocumentRepository;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService extends AbstractCrudService<Document, DocumentRepository> {

    private final DocumentRepository documentRepository;
    private final PetRepository petRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final JwtService jwtService;  // novo polje


    public DocumentService(DocumentRepository documentRepository,
                           PetRepository petRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService,
                           JwtService jwtService) {
        super(documentRepository);
        this.documentRepository = documentRepository;
        this.petRepository = petRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.jwtService = jwtService; 
    }

    @Override
    protected String getEntityName() {
        return "Document";
    }

    @Override
    protected Optional<Document> findByIdAndClinicId(UUID id, UUID clinicId) {
        return documentRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Document> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return documentRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return documentRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Document entity) {
        if (entity.getPetId() != null) {
            requireExists(
                    petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                    "Pet", "id", entity.getPetId()
            );
        }
        if (entity.getMedicalRecordId() != null) {
            requireExists(
                    medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getMedicalRecordId(), entity.getClinicId()),
                    "MedicalRecord", "id", entity.getMedicalRecordId()
            );
        }
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getUploadedBy(), entity.getClinicId()),
                "User", "id", entity.getUploadedBy()
        );
    }

    @Transactional(readOnly = true)
    public List<Document> findByPet(UUID clinicId, UUID petId) {
        return documentRepository.findByClinicIdAndPetIdAndDeletedFalse(clinicId, petId);
    }

    @Transactional(readOnly = true)
    public List<Document> findByMedicalRecord(UUID clinicId, UUID medicalRecordId) {
        return documentRepository.findByClinicIdAndMedicalRecordIdAndDeletedFalse(clinicId, medicalRecordId);
    }
    
    @Transactional
    public Document uploadFile(UUID id, UUID clinicId, MultipartFile file) {
    	Document document = findById(id, clinicId);
    	        
        fileStorageService.attachFile(document, file, "documents/" + clinicId);
        return repository.save(document);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(UUID id, UUID clinicId) {
    	Document document = findById(id, clinicId);

        if (document.getStoragePath() == null) {
            throw new ResourceNotFoundException("Fajl nije pronađen za dokument: " + id);
        }
        return fileStorageService.load(document.getStoragePath());
    }

    @Transactional
    public Document deleteFile(UUID id, UUID clinicId) {
    	Document document = findById(id, clinicId);
    	        
        fileStorageService.detachFile(document);
        return repository.save(document);
    }
    
    public Page<Document> searchAll(UUID clinicId, String search, Pageable pageable) {
		 if (search == null || search.isBlank()) {
		        return findAllByClinicId(clinicId, pageable);
		    }
        return documentRepository.searchByClinicId(clinicId, search, pageable);
    }

 // === QR Upload metode ===

    public String generateUploadToken(UUID petId, UUID clinicId, UUID userId) {
        var pet = petRepository.findByIdAndClinicIdAndDeletedFalse(petId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));

        return jwtService.generateUploadToken(petId, clinicId, userId, pet.getName());
    }

    @Transactional
    public Document uploadFromToken(String token, MultipartFile file, String description) {
        if (!jwtService.isUploadToken(token)) {
            throw new IllegalArgumentException("Nevažeći ili istekao upload token");
        }

        Claims claims = jwtService.parseToken(token);
        UUID petId = UUID.fromString(claims.get("petId", String.class));
        UUID clinicId = UUID.fromString(claims.get("clinicId", String.class));
        UUID uploadedBy = UUID.fromString(claims.get("uploadedBy", String.class));

        try {
            ClinicContextHolder.set(clinicId);

            Document document = new Document();
            document.setClinicId(clinicId);
            document.setPetId(petId);
            document.setUploadedBy(uploadedBy);
            document.setDescription(description);
            document.setDeleted(false);

            // Odredi fileType iz MIME tipa
            String mimeType = file.getContentType();
            if (mimeType != null && mimeType.startsWith("image/")) {
                document.setFileType(FileType.IMAGE);
            } else if ("application/pdf".equals(mimeType)) {
                document.setFileType(FileType.PDF);
            } else {
                document.setFileType(FileType.OTHER);
            }

            // attachFile postavlja fileName, mimeType, fileSizeBytes, storagePath
            fileStorageService.attachFile(document, file, "documents/" + clinicId);

            return repository.save(document);

        } finally {
            ClinicContextHolder.clear();
        }
    }

    public Map<String, Object> getUploadTokenInfo(String token) {
        if (!jwtService.isUploadToken(token)) {
            throw new IllegalArgumentException("Nevažeći ili istekao upload token");
        }

        Claims claims = jwtService.parseToken(token);
        Map<String, Object> info = new HashMap<>();
        info.put("petName", claims.get("petName", String.class));
        info.put("expiresAt", claims.getExpiration().toInstant().toString());
        info.put("valid", true);
        return info;
    }

    @Transactional
    public Document createWithFile(UUID clinicId, UUID petId, UUID uploadedBy, MultipartFile file, String description) {
        Document document = new Document();
        document.setClinicId(clinicId);
        document.setPetId(petId);
        document.setUploadedBy(uploadedBy);
        document.setDescription(description);
        document.setDeleted(false);

        String mimeType = file.getContentType();
        if (mimeType != null && mimeType.startsWith("image/")) {
            document.setFileType(FileType.IMAGE);
        } else if ("application/pdf".equals(mimeType)) {
            document.setFileType(FileType.PDF);
        } else {
            document.setFileType(FileType.OTHER);
        }

        fileStorageService.attachFile(document, file, "documents/" + clinicId);
        return repository.save(document);
    }

}
