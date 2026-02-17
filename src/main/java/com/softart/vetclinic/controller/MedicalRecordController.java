package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateMedicalRecordRequest;
import com.softart.vetclinic.dto.MedicalRecordResponse;
import com.softart.vetclinic.dto.UpdateMedicalRecordRequest;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.mapper.MedicalRecordMapper;
import com.softart.vetclinic.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final MedicalRecordMapper medicalRecordMapper;

    @GetMapping
    public Page<MedicalRecordResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return medicalRecordService.findAll(clinicId, pageable).map(medicalRecordMapper::toResponse);
    }

    @GetMapping("/{id}")
    public MedicalRecordResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return medicalRecordMapper.toResponse(medicalRecordService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRecordResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateMedicalRecordRequest request) {
        var entity = medicalRecordMapper.toEntity(request);
        return medicalRecordMapper.toResponse(medicalRecordService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public MedicalRecordResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {
        return medicalRecordMapper.toResponse(
                medicalRecordService.update(id, clinicId, existing -> medicalRecordMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        medicalRecordService.softDelete(id, clinicId);
    }

    @GetMapping("/by-pet/{petId}")
    public List<MedicalRecordResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return medicalRecordService.findByPet(clinicId, petId).stream()
                .map(medicalRecordMapper::toResponse).toList();
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public MedicalRecordResponse getByAppointment(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID appointmentId) {
        return medicalRecordService.findByAppointment(appointmentId)
                .map(medicalRecordMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "appointmentId", appointmentId));
    }
}
