package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateInventoryBatchRequest;
import com.softart.vetclinic.dto.InventoryBatchResponse;
import com.softart.vetclinic.dto.UpdateInventoryBatchRequest;
import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.mapper.InventoryBatchMapper;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.service.InventoryBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory-batches")
@RequiredArgsConstructor
public class InventoryBatchController {

    private final InventoryBatchService batchService;
    private final InventoryBatchMapper batchMapper;
    private final InventoryItemRepository itemRepository;

    @GetMapping("/by-item/{itemId}")
    public List<InventoryBatchResponse> getByItem(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID itemId) {
        return enrich(batchService.findByItem(clinicId, itemId), clinicId);
    }

    @GetMapping("/expiring")
    public List<InventoryBatchResponse> getExpiring(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(defaultValue = "30") int days) {
        return enrich(batchService.findExpiring(clinicId, days), clinicId);
    }

    @GetMapping("/expiring/count")
    public long getExpiringCount(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(defaultValue = "30") int days) {
        return batchService.countExpiring(clinicId, days);
    }

    @GetMapping("/{id}")
    public InventoryBatchResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        InventoryBatch batch = batchService.findById(id, clinicId);
        return enrich(List.of(batch), clinicId).get(0);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryBatchResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInventoryBatchRequest request) {
        InventoryBatch entity = batchMapper.toEntity(request);
        InventoryBatch saved = batchService.createBatch(entity, clinicId);
        return enrich(List.of(saved), clinicId).get(0);
    }

    @PutMapping("/{id}")
    public InventoryBatchResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryBatchRequest request) {
        InventoryBatch updated = batchService.update(id, clinicId, existing -> batchMapper.updateEntity(request, existing));
        return enrich(List.of(updated), clinicId).get(0);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        batchService.deleteBatch(id, clinicId);
    }

    /**
     * Batch-fetch artikala (name, unit) i izračunavanje daysUntilExpiry + status.
     */
    private List<InventoryBatchResponse> enrich(List<InventoryBatch> batches, UUID clinicId) {
        if (batches.isEmpty()) return List.of();

        Set<UUID> itemIds = batches.stream().map(InventoryBatch::getInventoryItemId).collect(Collectors.toSet());
        Map<UUID, InventoryItem> itemMap = new HashMap<>();
        for (UUID itemId : itemIds) {
            itemRepository.findByIdAndClinicIdAndDeletedFalse(itemId, clinicId)
                    .ifPresent(i -> itemMap.put(itemId, i));
        }

        LocalDate today = LocalDate.now();

        return batches.stream().map(b -> {
            InventoryBatchResponse base = batchMapper.toResponse(b);
            InventoryItem item = itemMap.get(b.getInventoryItemId());

            String itemName = item != null ? item.getName() : null;
            String itemUnit = item != null ? item.getUnit() : null;

            Long daysUntil = null;
            String status = "OK";
            if (b.getExpiryDate() != null) {
                long days = ChronoUnit.DAYS.between(today, b.getExpiryDate());
                daysUntil = days;
                if (days < 0) {
                    status = "EXPIRED";
                } else if (days <= 30) {
                    status = "EXPIRING_SOON";
                }
            }

            return new InventoryBatchResponse(
                    base.id(),
                    base.inventoryItemId(),
                    itemName,
                    itemUnit,
                    base.batchNumber(),
                    base.expiryDate(),
                    base.quantityOnHand(),
                    base.receivedAt(),
                    base.supplier(),
                    base.costPrice(),
                    base.notes(),
                    daysUntil,
                    status,
                    base.createdAt(),
                    base.updatedAt());
        }).toList();
    }
}
