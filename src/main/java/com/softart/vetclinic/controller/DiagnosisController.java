package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateDiagnosisRequest;
import com.softart.vetclinic.dto.DiagnosisResponse;
import com.softart.vetclinic.dto.UpdateDiagnosisRequest;
import com.softart.vetclinic.mapper.DiagnosisMapper;
import com.softart.vetclinic.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DiagnosisMapper diagnosisMapper;

    @GetMapping
    public Page<DiagnosisResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "name"));
        }
        return diagnosisService.searchAll(clinicId, search, pageable)
                .map(diagnosisMapper::toResponse);
    }

    @GetMapping("/autocomplete")
    public Page<DiagnosisResponse> autocomplete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "name"));
        }
        return diagnosisService.searchActive(clinicId, search, pageable)
                .map(diagnosisMapper::toResponse);
    }

    @GetMapping("/{id}")
    public DiagnosisResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return diagnosisMapper.toResponse(diagnosisService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateDiagnosisRequest request) {
        var entity = diagnosisMapper.toEntity(request);
        return diagnosisMapper.toResponse(diagnosisService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public DiagnosisResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiagnosisRequest request) {
        return diagnosisMapper.toResponse(
                diagnosisService.update(id, clinicId, existing -> diagnosisMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        diagnosisService.softDelete(id, clinicId);
    }
}
