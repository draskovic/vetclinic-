package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateLabReportRequest;
import com.softart.vetclinic.dto.LabReportResponse;
import com.softart.vetclinic.dto.UpdateLabReportRequest;
import com.softart.vetclinic.enums.LabReportStatus;
import com.softart.vetclinic.mapper.LabReportMapper;
import com.softart.vetclinic.service.LabReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import com.softart.vetclinic.dto.PdfParseResult;
import com.softart.vetclinic.service.PdfParserService;


@RestController
@RequestMapping("/api/lab-reports")
@RequiredArgsConstructor
public class LabReportController {

    private final LabReportService labReportService;
    private final LabReportMapper labReportMapper;
    private final PdfParserService pdfParserService;


    @GetMapping
    public Page<LabReportResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return labReportService.findAll(clinicId, pageable).map(labReportMapper::toResponse);
    }

    @GetMapping("/{id}")
    public LabReportResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return labReportMapper.toResponse(labReportService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabReportResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateLabReportRequest request) {
        var entity = labReportMapper.toEntity(request);
        return labReportMapper.toResponse(labReportService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public LabReportResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLabReportRequest request) {
        return labReportMapper.toResponse(
                labReportService.update(id, clinicId, existing -> labReportMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        labReportService.softDelete(id, clinicId);
    }

    @GetMapping("/by-pet/{petId}")
    public List<LabReportResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return labReportService.findByPet(clinicId, petId).stream()
                .map(labReportMapper::toResponse).toList();
    }

    @GetMapping("/by-status")
    public List<LabReportResponse> getByStatus(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam LabReportStatus status) {
        return labReportService.findByStatus(clinicId, status).stream()
                .map(labReportMapper::toResponse).toList();
    }

    @GetMapping("/by-vet/{vetId}")
    public List<LabReportResponse> getByVet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID vetId) {
        return labReportService.findByVet(clinicId, vetId).stream()
                .map(labReportMapper::toResponse).toList();
    }
    
    @PostMapping(value="/parse-pdf", consumes = "multipart/form-data")
    public PdfParseResult parsePdf(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam("file") MultipartFile file) {
        return pdfParserService.parsePdf(file, clinicId);
    }



    // ========================================================================
    // File endpoints
    // ========================================================================

    @PostMapping("/{id}/upload")
    public LabReportResponse uploadFile(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return labReportMapper.toResponse(labReportService.uploadFile(id, clinicId, file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        var labReport = labReportService.findById(id, clinicId);
        var resource = labReportService.downloadFile(id, clinicId);

        String contentType = labReport.getMimeType() != null ? labReport.getMimeType() : "application/octet-stream";
        String fileName = labReport.getFileName() != null ? labReport.getFileName() : "lab-report.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }

    @DeleteMapping("/{id}/file")
    public LabReportResponse deleteFile(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return labReportMapper.toResponse(labReportService.deleteFile(id, clinicId));
    }

}
