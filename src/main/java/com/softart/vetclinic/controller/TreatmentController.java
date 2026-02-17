package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateTreatmentRequest;
import com.softart.vetclinic.dto.TreatmentResponse;
import com.softart.vetclinic.dto.UpdateTreatmentRequest;
import com.softart.vetclinic.mapper.TreatmentMapper;
import com.softart.vetclinic.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final TreatmentMapper treatmentMapper;

    @GetMapping
    public Page<TreatmentResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return treatmentService.findAll(clinicId, pageable).map(treatmentMapper::toResponse);
    }

    @GetMapping("/{id}")
    public TreatmentResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return treatmentMapper.toResponse(treatmentService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateTreatmentRequest request) {
        var entity = treatmentMapper.toEntity(request);
        return treatmentMapper.toResponse(treatmentService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public TreatmentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreatmentRequest request) {
        return treatmentMapper.toResponse(
                treatmentService.update(id, clinicId, existing -> treatmentMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        treatmentService.softDelete(id, clinicId);
    }

    @GetMapping("/by-medical-record/{medicalRecordId}")
    public List<TreatmentResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        return treatmentService.findByMedicalRecord(clinicId, medicalRecordId).stream()
                .map(treatmentMapper::toResponse).toList();
    }
}
