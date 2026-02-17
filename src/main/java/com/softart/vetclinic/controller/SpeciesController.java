package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateSpeciesRequest;
import com.softart.vetclinic.dto.SpeciesResponse;
import com.softart.vetclinic.dto.UpdateSpeciesRequest;
import com.softart.vetclinic.mapper.SpeciesMapper;
import com.softart.vetclinic.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;
    private final SpeciesMapper speciesMapper;

    @GetMapping
    public Page<SpeciesResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return speciesService.findAll(clinicId, pageable).map(speciesMapper::toResponse);
    }

    @GetMapping("/{id}")
    public SpeciesResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return speciesMapper.toResponse(speciesService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpeciesResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateSpeciesRequest request) {
        var entity = speciesMapper.toEntity(request);
        return speciesMapper.toResponse(speciesService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public SpeciesResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSpeciesRequest request) {
        return speciesMapper.toResponse(
                speciesService.update(id, clinicId, existing -> speciesMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        speciesService.softDelete(id, clinicId);
    }
}
