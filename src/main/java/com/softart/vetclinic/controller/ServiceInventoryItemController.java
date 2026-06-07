package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateServiceInventoryItemRequest;
import com.softart.vetclinic.dto.ServiceInventoryItemResponse;
import com.softart.vetclinic.dto.UpdateServiceInventoryItemRequest;
import com.softart.vetclinic.mapper.ServiceInventoryItemMapper;
import com.softart.vetclinic.repository.ProductRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.service.ServiceInventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service-inventory-items")
@RequiredArgsConstructor
public class ServiceInventoryItemController {

    private final ServiceInventoryItemService service;
    private final ServiceInventoryItemMapper mapper;
    private final ServiceRepository serviceRepository;
    private final ProductRepository productRepository;

    @GetMapping("/by-service/{serviceId}")
    public List<ServiceInventoryItemResponse> getByService(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID serviceId) {
        var items = service.findByService(clinicId, serviceId);
        return resolveNames(items, clinicId);
    }

    @GetMapping("/by-product/{productId}")
    public List<ServiceInventoryItemResponse> getByProduct(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID productId) {
        var items = service.findByProduct(clinicId, productId);
        return resolveNames(items, clinicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceInventoryItemResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateServiceInventoryItemRequest request) {
        var entity = mapper.toEntity(request);
        var result = service.create(entity, clinicId);
        return resolveSingleName(result, clinicId);
    }

    @PutMapping("/{id}")
    public ServiceInventoryItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceInventoryItemRequest request) {
        var result = service.update(id, clinicId,
                existing -> mapper.updateEntity(request, existing));
        return resolveSingleName(result, clinicId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        service.softDelete(id, clinicId);
    }

    // --- Batch-fetch helper metode ---

    private List<ServiceInventoryItemResponse> resolveNames(
            List<com.softart.vetclinic.entity.ServiceInventoryItem> items, UUID clinicId) {

        var serviceIds = items.stream().map(i -> i.getServiceId()).distinct().toList();
        Map<UUID, com.softart.vetclinic.entity.Service> serviceMap = serviceIds.stream()
                .map(sid -> serviceRepository.findByIdAndClinicIdAndDeletedFalse(sid, clinicId).orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toMap(s -> s.getId(), s -> s));

        var productIds = items.stream().map(i -> i.getProductId()).distinct().toList();
        Map<UUID, com.softart.vetclinic.entity.Product> productMap = productIds.stream()
                .map(pid -> productRepository.findByIdAndClinicIdAndDeletedFalse(pid, clinicId).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toMap(p -> p.getId(), p -> p));

        return items.stream().map(item -> {
            var response = mapper.toResponse(item);
            var svc = serviceMap.get(item.getServiceId());
            var prod = productMap.get(item.getProductId());
            return new ServiceInventoryItemResponse(
                response.id(),
                response.serviceId(),
                svc != null ? svc.getName() : null,
                response.productId(),
                prod != null ? prod.getName() : null,
                response.quantityPerUse(),
                prod != null ? prod.getUnit() : null,
                response.createdAt(),
                response.updatedAt()
            );
        }).toList();
    }

    private ServiceInventoryItemResponse resolveSingleName(
            com.softart.vetclinic.entity.ServiceInventoryItem item, UUID clinicId) {
        var response = mapper.toResponse(item);
        String serviceName = serviceRepository.findByIdAndClinicIdAndDeletedFalse(item.getServiceId(), clinicId)
                .map(s -> s.getName()).orElse(null);
        var prod = productRepository.findByIdAndClinicIdAndDeletedFalse(item.getProductId(), clinicId)
                .orElse(null);
        return new ServiceInventoryItemResponse(
            response.id(),
            response.serviceId(),
            serviceName,
            response.productId(),
            prod != null ? prod.getName() : null,
            response.quantityPerUse(),
            prod != null ? prod.getUnit() : null,
            response.createdAt(),
            response.updatedAt()
        );
    }
}