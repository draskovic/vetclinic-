package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateServiceRequest;
import com.softart.vetclinic.dto.ImportResultResponse;
import com.softart.vetclinic.dto.ImportServiceRequest;
import com.softart.vetclinic.dto.ServiceResponse;
import com.softart.vetclinic.dto.UpdateServiceRequest;
import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.enums.ServiceCategory;
import com.softart.vetclinic.mapper.ServiceMapper;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.TaxRateRepository;
import com.softart.vetclinic.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ClinicServiceController {

    private final ServiceService serviceService;
    private final ServiceMapper serviceMapper;
    private final com.softart.vetclinic.service.ServiceImportService serviceImportService;
    private final TaxRateRepository taxRateRepository;
    private final ClinicRepository clinicRepository;

    @GetMapping
    public Page<ServiceResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ServiceCategory category,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "name"));
        }
        Page<ServiceResponse> page = serviceService.searchAll(clinicId, search, category, pageable)
                .map(serviceMapper::toResponse);
        List<ServiceResponse> enriched = enrichMany(page.getContent());
        return new PageImpl<>(enriched, pageable, page.getTotalElements());
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResultResponse importServices(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestBody List<ImportServiceRequest> requests) {
        return serviceImportService.importServices(clinicId, requests);
    }

    @GetMapping("/{id}")
    public ServiceResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return enrichOne(serviceMapper.toResponse(serviceService.findById(id, clinicId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateServiceRequest request) {
        var entity = serviceMapper.toEntity(request);
        if (entity.getTaxRateId() == null) {
            entity.setTaxRateId(resolveDefaultTaxRateId(clinicId));
        }
        return enrichOne(serviceMapper.toResponse(serviceService.create(entity, clinicId)));
    }

    @PutMapping("/{id}")
    public ServiceResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return enrichOne(serviceMapper.toResponse(
                serviceService.update(id, clinicId, existing -> serviceMapper.updateEntity(request, existing))));
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
        return enrichMany(serviceService.findByCategory(clinicId, category).stream()
                .map(serviceMapper::toResponse).toList());
    }

    /**
     * Batch-fetch taxRateLabel i taxRatePercent za listu ServiceResponse.
     */
    private List<ServiceResponse> enrichMany(List<ServiceResponse> items) {
        Set<UUID> taxRateIds = items.stream()
                .map(ServiceResponse::taxRateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TaxRate> taxRates = taxRateIds.isEmpty()
                ? Map.of()
                : taxRateRepository.findAllById(taxRateIds).stream()
                        .collect(Collectors.toMap(TaxRate::getId, tr -> tr));

        return items.stream().map(r -> {
            TaxRate tr = r.taxRateId() != null ? taxRates.get(r.taxRateId()) : null;
            return new ServiceResponse(
                    r.id(),
                    r.category(),
                    r.name(),
                    r.sku(),
                    r.unit(),
                    r.description(),
                    r.price(),
                    r.taxRateId(),
                    tr != null ? tr.getLabel() : null,
                    tr != null ? tr.getPercent() : null,
                    r.durationMinutes(),
                    r.active(),
                    r.createdAt(),
                    r.updatedAt()
            );
        }).toList();
    }

    private ServiceResponse enrichOne(ServiceResponse response) {
        return enrichMany(List.of(response)).get(0);
    }

    private UUID resolveDefaultTaxRateId(UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalStateException("Klinika ne postoji: " + clinicId));
        String label = Boolean.TRUE.equals(clinic.getVatPayer()) ? "Ђ" : "А";
        TaxRate taxRate = taxRateRepository.findByCountryCodeAndLabel("RS", label)
                .orElseThrow(() -> new IllegalStateException(
                        "Default TaxRate '" + label + "' (RS) nije pronađen u šifarniku"));
        return taxRate.getId();
    }
}