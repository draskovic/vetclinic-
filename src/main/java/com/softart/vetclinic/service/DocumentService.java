package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Document;
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

    public DocumentService(DocumentRepository documentRepository,
                           PetRepository petRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService) {
        super(documentRepository);
        this.documentRepository = documentRepository;
        this.petRepository = petRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
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
}
