package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateOwnerRequest;
import com.softart.vetclinic.dto.OwnerResponse;
import com.softart.vetclinic.dto.UpdateOwnerRequest;
import com.softart.vetclinic.mapper.OwnerMapper;
import com.softart.vetclinic.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;
    private final OwnerMapper ownerMapper;

    @GetMapping
    public Page<OwnerResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return ownerService.findAll(clinicId, pageable).map(ownerMapper::toResponse);
    }

    @GetMapping("/{id}")
    public OwnerResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return ownerMapper.toResponse(ownerService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateOwnerRequest request) {
        var entity = ownerMapper.toEntity(request);
        return ownerMapper.toResponse(ownerService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public OwnerResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOwnerRequest request) {
        return ownerMapper.toResponse(
                ownerService.update(id, clinicId, existing -> ownerMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        ownerService.softDelete(id, clinicId);
    }

    @GetMapping("/search/by-last-name")
    public List<OwnerResponse> searchByLastName(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam String lastName) {
        return ownerService.searchByLastName(clinicId, lastName).stream()
                .map(ownerMapper::toResponse).toList();
    }

    @GetMapping("/search/by-phone")
    public List<OwnerResponse> searchByPhone(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam String phone) {
        return ownerService.searchByPhone(clinicId, phone).stream()
                .map(ownerMapper::toResponse).toList();
    }
}
