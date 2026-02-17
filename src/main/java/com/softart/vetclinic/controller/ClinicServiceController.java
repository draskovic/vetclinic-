package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateServiceRequest;
import com.softart.vetclinic.dto.ServiceResponse;
import com.softart.vetclinic.dto.UpdateServiceRequest;
import com.softart.vetclinic.enums.ServiceCategory;
import com.softart.vetclinic.mapper.ServiceMapper;
import com.softart.vetclinic.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ClinicServiceController {

    private final ServiceService serviceService;
    private final ServiceMapper serviceMapper;

    @GetMapping
    public Page<ServiceResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return serviceService.findAll(clinicId, pageable).map(serviceMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ServiceResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return serviceMapper.toResponse(serviceService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateServiceRequest request) {
        var entity = serviceMapper.toEntity(request);
        return serviceMapper.toResponse(serviceService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public ServiceResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return serviceMapper.toResponse(
                serviceService.update(id, clinicId, existing -> serviceMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        serviceService.softDelete(id, clinicId);
    }

    @GetMapping("/by-category/{category}")
    public List<ServiceResponse> getByCategory(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable ServiceCategory category) {
        return serviceService.findByCategory(clinicId, category).stream()
                .map(serviceMapper::toResponse).toList();
    }
}
