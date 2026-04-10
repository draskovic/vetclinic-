package com.softart.vetclinic.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.softart.vetclinic.dto.CreateInventoryItemRequest;
import com.softart.vetclinic.dto.InventoryItemResponse;
import com.softart.vetclinic.dto.UpdateInventoryItemRequest;
import com.softart.vetclinic.entity.ClinicLocation;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.mapper.InventoryItemMapper;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import com.softart.vetclinic.service.InventoryItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory-items")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;
    private final InventoryItemMapper inventoryItemMapper;
    private final ClinicLocationRepository clinicLocationRepository;

    @GetMapping
    public Page<InventoryItemResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InventoryCategory category,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "name"));
        }
        Page<InventoryItemResponse> page = inventoryItemService.searchAll(clinicId, search, category, pageable)
                .map(inventoryItemMapper::toResponse);
        List<InventoryItemResponse> enriched = enrichMany(page.getContent());
        return new org.springframework.data.domain.PageImpl<>(enriched, pageable, page.getTotalElements());

    }


    @GetMapping("/{id}")
    public InventoryItemResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
    	return enrichOne(inventoryItemMapper.toResponse(inventoryItemService.findById(id, clinicId)));

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        var entity = inventoryItemMapper.toEntity(request);
        return enrichOne(inventoryItemMapper.toResponse(inventoryItemService.create(entity, clinicId)));

    }

    @PutMapping("/{id}")
    public InventoryItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
    	return enrichOne(inventoryItemMapper.toResponse(
    	        inventoryItemService.update(id, clinicId, existing -> inventoryItemMapper.updateEntity(request, existing))));

    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        inventoryItemService.softDelete(id, clinicId);
    }
    
    @GetMapping("/low-stock")
    public List<InventoryItemResponse> getLowStock(
            @RequestHeader("X-Clinic-Id") UUID clinicId) {
    	return enrichMany(inventoryItemService.findLowStock(clinicId).stream()
    	        .map(inventoryItemMapper::toResponse).toList());

    }

    @GetMapping("/low-stock/count")
    public long getLowStockCount(
            @RequestHeader("X-Clinic-Id") UUID clinicId) {
        return inventoryItemService.countLowStock(clinicId);
    }

    @GetMapping("/by-category/{category}")
    public List<InventoryItemResponse> getByCategory(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable InventoryCategory category) {
    	return enrichMany(inventoryItemService.findByCategory(clinicId, category).stream()
    	        .map(inventoryItemMapper::toResponse).toList());

    }
    
    /**
     * Batch-fetch locationName za listu InventoryItemResponse.
     * Koristi se umesto MapStruct source="location.name" jer lazy load puca "no session" sa RLS.
     */
    private List<InventoryItemResponse> enrichMany(List<InventoryItemResponse> items) {
        Set<UUID> locationIds = items.stream()
                .map(InventoryItemResponse::locationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> locationNames = locationIds.isEmpty()
                ? Map.of()
                : clinicLocationRepository.findAllById(locationIds).stream()
                        .collect(Collectors.toMap(ClinicLocation::getId, ClinicLocation::getName));

        return items.stream().map(r -> new InventoryItemResponse(
                r.id(),
                r.locationId(),
                r.locationId() != null ? locationNames.get(r.locationId()) : null,
                r.name(),
                r.sku(),
                r.category(),
                r.quantityOnHand(),
                r.unit(),
                r.reorderLevel(),
                r.costPrice(),
                r.sellPrice(),
                r.expiryDate(),
                r.active(),
                r.trackBatches(),
                r.createdAt(),
                r.updatedAt()
        )).toList();
    }

    private InventoryItemResponse enrichOne(InventoryItemResponse response) {
        return enrichMany(List.of(response)).get(0);
    }

}
