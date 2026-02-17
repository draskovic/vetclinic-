package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreatePrescriptionRequest;
import com.softart.vetclinic.dto.PrescriptionResponse;
import com.softart.vetclinic.dto.UpdatePrescriptionRequest;
import com.softart.vetclinic.mapper.PrescriptionMapper;
import com.softart.vetclinic.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionMapper prescriptionMapper;

    @GetMapping
    public Page<PrescriptionResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return prescriptionService.findAll(clinicId, pageable).map(prescriptionMapper::toResponse);
    }

    @GetMapping("/{id}")
    public PrescriptionResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return prescriptionMapper.toResponse(prescriptionService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreatePrescriptionRequest request) {
        var entity = prescriptionMapper.toEntity(request);
        return prescriptionMapper.toResponse(prescriptionService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public PrescriptionResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePrescriptionRequest request) {
        return prescriptionMapper.toResponse(
                prescriptionService.update(id, clinicId, existing -> prescriptionMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        prescriptionService.softDelete(id, clinicId);
    }

    @GetMapping("/by-medical-record/{medicalRecordId}")
    public List<PrescriptionResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        return prescriptionService.findByMedicalRecord(clinicId, medicalRecordId).stream()
                .map(prescriptionMapper::toResponse).toList();
    }

    @GetMapping("/by-pet/{petId}")
    public List<PrescriptionResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return prescriptionService.findByPet(clinicId, petId).stream()
                .map(prescriptionMapper::toResponse).toList();
    }
}
