package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.ClinicLocationResponse;
import com.softart.vetclinic.dto.CreateClinicLocationRequest;
import com.softart.vetclinic.dto.UpdateClinicLocationRequest;
import com.softart.vetclinic.mapper.ClinicLocationMapper;
import com.softart.vetclinic.service.ClinicLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinic-locations")
@RequiredArgsConstructor
public class ClinicLocationController {

    private final ClinicLocationService clinicLocationService;
    private final ClinicLocationMapper clinicLocationMapper;

    @GetMapping
    public Page<ClinicLocationResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return clinicLocationService.findAll(clinicId, pageable).map(clinicLocationMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ClinicLocationResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return clinicLocationMapper.toResponse(clinicLocationService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicLocationResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateClinicLocationRequest request) {
        var entity = clinicLocationMapper.toEntity(request);
        return clinicLocationMapper.toResponse(clinicLocationService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public ClinicLocationResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicLocationRequest request) {
        return clinicLocationMapper.toResponse(
                clinicLocationService.update(id, clinicId, existing -> clinicLocationMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        clinicLocationService.softDelete(id, clinicId);
    }

    @GetMapping("/active")
    public List<ClinicLocationResponse> getActive(
            @RequestHeader("X-Clinic-Id") UUID clinicId) {
        return clinicLocationService.findActiveByClinic(clinicId).stream()
                .map(clinicLocationMapper::toResponse).toList();
    }
}
