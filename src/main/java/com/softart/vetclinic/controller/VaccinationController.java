package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateVaccinationRequest;
import com.softart.vetclinic.dto.UpdateVaccinationRequest;
import com.softart.vetclinic.dto.VaccinationResponse;
import com.softart.vetclinic.mapper.VaccinationMapper;
import com.softart.vetclinic.service.VaccinationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vaccinations")
@RequiredArgsConstructor
public class VaccinationController {

    private final VaccinationService vaccinationService;
    private final VaccinationMapper vaccinationMapper;

    @GetMapping
    public Page<VaccinationResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return vaccinationService.findAll(clinicId, pageable).map(vaccinationMapper::toResponse);
    }

    @GetMapping("/{id}")
    public VaccinationResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return vaccinationMapper.toResponse(vaccinationService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VaccinationResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateVaccinationRequest request) {
        var entity = vaccinationMapper.toEntity(request);
        return vaccinationMapper.toResponse(vaccinationService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public VaccinationResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVaccinationRequest request) {
        return vaccinationMapper.toResponse(
                vaccinationService.update(id, clinicId, existing -> vaccinationMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        vaccinationService.softDelete(id, clinicId);
    }

    @GetMapping("/by-pet/{petId}")
    public List<VaccinationResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return vaccinationService.findByPet(clinicId, petId).stream()
                .map(vaccinationMapper::toResponse).toList();
    }

    @GetMapping("/due")
    public List<VaccinationResponse> getDueVaccinations(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before) {
        return vaccinationService.findDueVaccinations(clinicId, before).stream()
                .map(vaccinationMapper::toResponse).toList();
    }
}
