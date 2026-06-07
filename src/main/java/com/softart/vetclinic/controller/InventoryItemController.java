package com.softart.vetclinic.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.softart.vetclinic.dto.CreateInventoryItemRequest;
import com.softart.vetclinic.dto.InventoryItemResponse;
import com.softart.vetclinic.dto.UpdateInventoryItemRequest;
import com.softart.vetclinic.entity.ClinicLocation;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.Product;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.mapper.InventoryItemMapper;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import com.softart.vetclinic.repository.ProductRepository;
import com.softart.vetclinic.repository.TaxRateRepository;
import com.softart.vetclinic.service.InventoryBatchService;
import com.softart.vetclinic.service.InventoryItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory-items")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;
    private final InventoryItemMapper inventoryItemMapper;
    private final ProductRepository productRepository;
    private final InventoryBatchService inventoryBatchService;
    private final ClinicLocationRepository clinicLocationRepository;
    private final TaxRateRepository taxRateRepository;

    @GetMapping
    public Page<InventoryItemResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InventoryCategory category,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "product.name"));
        }
        Page<InventoryItem> page = inventoryItemService.searchAll(clinicId, search, category, pageable);
        List<InventoryItemResponse> enriched = enrichMany(clinicId, page.getContent());
        return new PageImpl<>(enriched, pageable, page.getTotalElements());
    }

    @GetMapping("/{id}")
    public InventoryItemResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return enrichOne(clinicId, inventoryItemService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        var entity = inventoryItemMapper.toEntity(request);
        var saved = inventoryItemService.createWithInitialQuantity(entity, clinicId, request.initialQuantity());
        return enrichOne(clinicId, saved);
    }

    @PutMapping("/{id}")
    public InventoryItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        return enrichOne(clinicId, inventoryItemService.update(id, clinicId,
                existing -> inventoryItemMapper.updateEntity(request, existing)));
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
        return enrichMany(clinicId, inventoryItemService.findLowStock(clinicId));
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
        return enrichMany(clinicId, inventoryItemService.findByCategory(clinicId, category));
    }

    /**
     * Batch-fetch product (name/sku/category/unit/trackBatches) + computed quantity (SUM lotova)
     * + locationName + tax (label/percent). Bez lazy navigacije (RLS-safe).
     */
    private List<InventoryItemResponse> enrichMany(UUID clinicId, List<InventoryItem> items) {
        if (items.isEmpty()) return List.of();

        Set<UUID> productIds = items.stream().map(InventoryItem::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Product> products = productIds.isEmpty() ? Map.of()
                : productRepository.findAllById(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        Set<UUID> locationIds = items.stream().map(InventoryItem::getLocationId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> locationNames = locationIds.isEmpty() ? Map.of()
                : clinicLocationRepository.findAllById(locationIds).stream()
                        .collect(Collectors.toMap(ClinicLocation::getId, ClinicLocation::getName));

        Set<UUID> taxRateIds = items.stream().map(InventoryItem::getTaxRateId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, TaxRate> taxRates = taxRateIds.isEmpty() ? Map.of()
                : taxRateRepository.findAllById(taxRateIds).stream()
                        .collect(Collectors.toMap(TaxRate::getId, tr -> tr));

        Set<UUID> itemIds = items.stream().map(InventoryItem::getId).collect(Collectors.toSet());
        Map<UUID, BigDecimal> quantities = inventoryBatchService.getQuantitiesByItemIds(clinicId, itemIds);

        return items.stream().map(i -> {
            Product p = i.getProductId() != null ? products.get(i.getProductId()) : null;
            TaxRate tr = i.getTaxRateId() != null ? taxRates.get(i.getTaxRateId()) : null;
            return new InventoryItemResponse(
                    i.getId(),
                    i.getProductId(),
                    i.getLocationId(),
                    i.getLocationId() != null ? locationNames.get(i.getLocationId()) : null,
                    p != null ? p.getName() : null,
                    p != null ? p.getSku() : null,
                    p != null ? p.getCategory() : null,
                    quantities.getOrDefault(i.getId(), BigDecimal.ZERO),
                    p != null ? p.getUnit() : null,
                    i.getReorderLevel(),
                    i.getSellPrice(),
                    i.getActive(),
                    p != null ? p.getTrackBatches() : null,
                    i.getTaxRateId(),
                    tr != null ? tr.getLabel() : null,
                    tr != null ? tr.getPercent() : null,
                    i.getCreatedAt(),
                    i.getUpdatedAt()
            );
        }).toList();
    }

    private InventoryItemResponse enrichOne(UUID clinicId, InventoryItem item) {
        return enrichMany(clinicId, List.of(item)).get(0);
    }
}