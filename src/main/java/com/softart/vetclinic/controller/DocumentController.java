package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateDocumentRequest;
import com.softart.vetclinic.dto.DocumentResponse;
import com.softart.vetclinic.dto.UpdateDocumentRequest;
import com.softart.vetclinic.mapper.DocumentMapper;
import com.softart.vetclinic.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}
