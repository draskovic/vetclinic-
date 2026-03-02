package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreatePetRequest;
import com.softart.vetclinic.dto.PetResponse;
import com.softart.vetclinic.dto.UpdatePetRequest;
import com.softart.vetclinic.mapper.PetMapper;
import com.softart.vetclinic.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final PetMapper petMapper;

    @GetMapping
    public Page<PetResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return petService.searchAll(clinicId, search, pageable).map(petMapper::toResponse);
    }


    @GetMapping("/{id}")
    public PetResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return petMapper.toResponse(petService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreatePetRequest request) {
        var entity = petMapper.toEntity(request);
        return petMapper.toResponse(petService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public PetResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePetRequest request) {
        return petMapper.toResponse(
                petService.update(id, clinicId, existing -> petMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        petService.softDelete(id, clinicId);
    }

    @GetMapping("/by-owner/{ownerId}")
    public List<PetResponse> getByOwner(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID ownerId) {
        return petService.findByOwner(clinicId, ownerId).stream()
                .map(petMapper::toResponse).toList();
    }
}
