package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateInventoryItemRequest;
import com.softart.vetclinic.dto.InventoryItemResponse;
import com.softart.vetclinic.dto.UpdateInventoryItemRequest;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.mapper.InventoryItemMapper;
import com.softart.vetclinic.service.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory-items")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;
    private final InventoryItemMapper inventoryItemMapper;

    @GetMapping
    public Page<InventoryItemResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return inventoryItemService.findAll(clinicId, pageable).map(inventoryItemMapper::toResponse);
    }

    @GetMapping("/{id}")
    public InventoryItemResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return inventoryItemMapper.toResponse(inventoryItemService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        var entity = inventoryItemMapper.toEntity(request);
        return inventoryItemMapper.toResponse(inventoryItemService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public InventoryItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        return inventoryItemMapper.toResponse(
                inventoryItemService.update(id, clinicId, existing -> inventoryItemMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        inventoryItemService.softDelete(id, clinicId);
    }

    @GetMapping("/by-category/{category}")
    public List<InventoryItemResponse> getByCategory(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable InventoryCategory category) {
        return inventoryItemService.findByCategory(clinicId, category).stream()
                .map(inventoryItemMapper::toResponse).toList();
    }
}
