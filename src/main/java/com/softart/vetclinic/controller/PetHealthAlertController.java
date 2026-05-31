package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreatePetHealthAlertRequest;
import com.softart.vetclinic.dto.PetHealthAlertResponse;
import com.softart.vetclinic.dto.UpdatePetHealthAlertRequest;
import com.softart.vetclinic.mapper.PetHealthAlertMapper;
import com.softart.vetclinic.service.PetHealthAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pet-health-alerts")
@RequiredArgsConstructor
public class PetHealthAlertController {

    private final PetHealthAlertService service;
    private final PetHealthAlertMapper mapper;

    @GetMapping
    public Page<PetHealthAlertResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return service.findAll(clinicId, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public PetHealthAlertResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return mapper.toResponse(service.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetHealthAlertResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreatePetHealthAlertRequest request) {
        var entity = mapper.toEntity(request);
        // Default active=true ako klijent ne pošalje eksplicitno
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        return mapper.toResponse(service.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public PetHealthAlertResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePetHealthAlertRequest request) {
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

    /**
     * Lista alert-a za pet-a.
     * - `activeOnly=true` (default): samo aktivni → koristi se za banner u 4 UI lokacije.
     * - `activeOnly=false`: sve alert-i uključujući deaktivirane → za editor modal.
     */
    @GetMapping("/by-pet/{petId}")
    public List<PetHealthAlertResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        var alerts = activeOnly
                ? service.findActiveByPet(clinicId, petId)
                : service.findAllByPet(clinicId, petId);
        return alerts.stream().map(mapper::toResponse).toList();
    }
}