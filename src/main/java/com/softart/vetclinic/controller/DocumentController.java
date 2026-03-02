package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateDocumentRequest;
import com.softart.vetclinic.dto.DocumentResponse;
import com.softart.vetclinic.dto.UpdateDocumentRequest;
import com.softart.vetclinic.entity.Document;
import com.softart.vetclinic.mapper.DocumentMapper;
import com.softart.vetclinic.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @GetMapping
    public Page<DocumentResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return documentService.findAll(clinicId, pageable).map(documentMapper::toResponse);
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return documentMapper.toResponse(documentService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateDocumentRequest request) {
        var entity = documentMapper.toEntity(request);
        return documentMapper.toResponse(documentService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public DocumentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentRequest request) {
        return documentMapper.toResponse(
                documentService.update(id, clinicId, existing -> documentMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        documentService.softDelete(id, clinicId);
    }

    @GetMapping("/by-pet/{petId}")
    public List<DocumentResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return documentService.findByPet(clinicId, petId).stream()
                .map(documentMapper::toResponse).toList();
    }

    @GetMapping("/by-medical-record/{medicalRecordId}")
    public List<DocumentResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        return documentService.findByMedicalRecord(clinicId, medicalRecordId).stream()
                .map(documentMapper::toResponse).toList();
    }
    
    @PostMapping("/{id}/upload")
    public ResponseEntity<DocumentResponse> uploadFile(
            @PathVariable UUID id,
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam("file") MultipartFile file) {
        Document document = documentService.uploadFile(id, clinicId, file);
        return ResponseEntity.ok(documentMapper.toResponse(document));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID id,
            @RequestHeader("X-Clinic-Id") UUID clinicId) {
        Document document = documentService.findById(id, clinicId);
        Resource resource = documentService.downloadFile(id, clinicId);
        
        String contentType = document.getMimeType() != null 
                ? document.getMimeType() 
                : "application/octet-stream";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/file")
    public ResponseEntity<DocumentResponse> deleteFile(
            @PathVariable UUID id,
            @RequestHeader("X-Clinic-Id") UUID clinicId) {
        Document document = documentService.deleteFile(id, clinicId);
        return ResponseEntity.ok(documentMapper.toResponse(document));
    }
}
