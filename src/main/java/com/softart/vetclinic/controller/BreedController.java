package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.BreedResponse;
import com.softart.vetclinic.dto.CreateBreedRequest;
import com.softart.vetclinic.dto.UpdateBreedRequest;
import com.softart.vetclinic.mapper.BreedMapper;
import com.softart.vetclinic.service.BreedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/breeds")
@RequiredArgsConstructor
public class BreedController {

    private final BreedService breedService;
    private final BreedMapper breedMapper;

    @GetMapping
    public Page<BreedResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return breedService.findAll(clinicId, pageable).map(breedMapper::toResponse);
    }

    @GetMapping("/{id}")
    public BreedResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return breedMapper.toResponse(breedService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BreedResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateBreedRequest request) {
        var entity = breedMapper.toEntity(request);
        return breedMapper.toResponse(breedService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public BreedResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBreedRequest request) {
        return breedMapper.toResponse(
                breedService.update(id, clinicId, existing -> breedMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        breedService.softDelete(id, clinicId);
    }

    @GetMapping("/by-species/{speciesId}")
    public List<BreedResponse> getBySpecies(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID speciesId) {
        return breedService.findBySpecies(clinicId, speciesId).stream()
                .map(breedMapper::toResponse).toList();
    }
}
