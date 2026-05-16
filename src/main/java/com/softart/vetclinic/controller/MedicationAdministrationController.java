package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateMedicationAdministrationRequest;

import com.softart.vetclinic.dto.MedicationAdministrationResponse;
import com.softart.vetclinic.dto.UpdateMedicationAdministrationRequest;
import com.softart.vetclinic.dto.MedicationQuickPicksResponse;
import com.softart.vetclinic.mapper.MedicationAdministrationMapper;
import com.softart.vetclinic.service.MedicationAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medication-administrations")
@RequiredArgsConstructor
public class MedicationAdministrationController {

    private final MedicationAdministrationService service;
    private final MedicationAdministrationMapper mapper;

    @GetMapping
    public Page<MedicationAdministrationResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return service.findAll(clinicId, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public MedicationAdministrationResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return mapper.toResponse(service.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationAdministrationResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateMedicationAdministrationRequest request) {
        var entity = mapper.toEntity(request);
        return mapper.toResponse(service.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public MedicationAdministrationResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicationAdministrationRequest request) {
        return mapper.toResponse(
                service.update(id, clinicId, existing -> mapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        service.softDelete(id, clinicId);
    }

    @GetMapping("/by-medical-record/{medicalRecordId}")
    public List<MedicationAdministrationResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        return service.findByMedicalRecord(clinicId, medicalRecordId).stream()
                .map(mapper::toResponse).toList();
    }

    @GetMapping("/by-pet/{petId}")
    public List<MedicationAdministrationResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return service.findByPet(clinicId, petId).stream()
                .map(mapper::toResponse).toList();
    }
    
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MedicationAdministrationResponse> createBulk(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody List<@Valid CreateMedicationAdministrationRequest> requests) {
        var entities = requests.stream().map(mapper::toEntity).toList();
        return service.createBulk(entities, clinicId).stream()
                .map(mapper::toResponse).toList();
    }

    @GetMapping("/quick-picks")
    public MedicationQuickPicksResponse getQuickPicks(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(defaultValue = "10") int limit) {
        return service.getQuickPicks(clinicId, limit);
    }
}