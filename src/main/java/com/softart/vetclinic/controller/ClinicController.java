package com.softart.vetclinic.controller;

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.dto.ClinicResponse;

import com.softart.vetclinic.dto.ProvisionClinicRequest;
import com.softart.vetclinic.dto.ProvisionClinicResponse;
import com.softart.vetclinic.service.ClinicProvisioningService;

import com.softart.vetclinic.dto.CreateClinicRequest;
import com.softart.vetclinic.dto.UpdateClinicRequest;
import com.softart.vetclinic.mapper.ClinicMapper;
import com.softart.vetclinic.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.softart.vetclinic.service.FileStorageService;
import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.exception.BadRequestException;


@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;
    private final ClinicMapper clinicMapper;
    private final ClinicProvisioningService clinicProvisioningService;
    private final FileStorageService fileStorageService;



    @GetMapping
    public Page<ClinicResponse> getAll(Pageable pageable) {
        return clinicService.findAll(pageable).map(clinicMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ClinicResponse getById(@PathVariable UUID id) {
        return clinicMapper.toResponse(clinicService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicResponse create(@Valid @RequestBody CreateClinicRequest request) {
        var entity = clinicMapper.toEntity(request);
        return clinicMapper.toResponse(clinicService.create(entity));
    }

    @PutMapping("/{id}")
    public ClinicResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest request) {
        return clinicMapper.toResponse(
                clinicService.update(id, existing -> clinicMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        clinicService.softDelete(id);
    }
    
    @PostMapping("/provision")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionClinicResponse provision(@Valid @RequestBody ProvisionClinicRequest request) {
        UUID clinicId = UUID.randomUUID();
        ClinicContextHolder.set(clinicId);
        try {
            return clinicProvisioningService.provision(clinicId, request);
        } finally {
            ClinicContextHolder.clear();
        }
    }

    @GetMapping("/lookup")
    public ClinicResponse getByEmail(@RequestParam String email) {
        return clinicMapper.toResponse(clinicService.findByEmail(email));
    }
    
    @PostMapping("/{id}/logo")
    public ClinicResponse uploadLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        // Validacija — samo slike (ne PDF)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Logo mora biti slika (PNG, JPEG, GIF, WEBP)");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BadRequestException("Logo ne sme biti veći od 2MB");
        }

        return clinicMapper.toResponse(clinicService.update(id, existing -> {
            // Obriši stari logo ako postoji
            if (existing.getLogoUrl() != null && !existing.getLogoUrl().isBlank()) {
                fileStorageService.delete(existing.getLogoUrl());
            }
            String storagePath = fileStorageService.save(file, "clinic-logos");
            existing.setLogoUrl(storagePath);
        }));
    }

    @DeleteMapping("/{id}/logo")
    public ClinicResponse deleteLogo(@PathVariable UUID id) {
        return clinicMapper.toResponse(clinicService.update(id, existing -> {
            if (existing.getLogoUrl() != null && !existing.getLogoUrl().isBlank()) {
                fileStorageService.delete(existing.getLogoUrl());
                existing.setLogoUrl(null);
            }
        }));
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> getLogo(@PathVariable UUID id) {
        Clinic clinic = clinicService.findById(id);
        if (clinic.getLogoUrl() == null || clinic.getLogoUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = fileStorageService.load(clinic.getLogoUrl());
        String contentType = "image/png";
        String path = clinic.getLogoUrl().toLowerCase();
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (path.endsWith(".gif")) contentType = "image/gif";
        else if (path.endsWith(".webp")) contentType = "image/webp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }


}
