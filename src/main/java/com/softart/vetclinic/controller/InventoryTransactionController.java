package com.softart.vetclinic.controller;

import java.util.List;
import java.util.UUID;

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

import com.softart.vetclinic.dto.CreateInventoryTransactionRequest;
import com.softart.vetclinic.dto.InventoryTransactionResponse;
import com.softart.vetclinic.dto.UpdateInventoryTransactionRequest;
import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.mapper.InventoryTransactionMapper;
import com.softart.vetclinic.service.InventoryTransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService inventoryTransactionService;
    private final InventoryTransactionMapper inventoryTransactionMapper;

    @GetMapping
    public Page<InventoryTransactionResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(required = false) UUID inventoryItemId,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return inventoryTransactionService.searchAll(clinicId, search, type, inventoryItemId, pageable)
                .map(inventoryTransactionMapper::toResponse);
    }


    @GetMapping("/{id}")
    public InventoryTransactionResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return inventoryTransactionMapper.toResponse(inventoryTransactionService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTransactionResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInventoryTransactionRequest request) {
        var entity = inventoryTransactionMapper.toEntity(request);
        return inventoryTransactionMapper.toResponse(inventoryTransactionService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public InventoryTransactionResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryTransactionRequest request) {
        return inventoryTransactionMapper.toResponse(
                inventoryTransactionService.update(id, clinicId, existing -> inventoryTransactionMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        inventoryTransactionService.softDelete(id, clinicId);
    }

    @GetMapping("/by-item/{inventoryItemId}")
    public List<InventoryTransactionResponse> getByItem(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID inventoryItemId) {
        return inventoryTransactionService.findByItem(clinicId, inventoryItemId).stream()
                .map(inventoryTransactionMapper::toResponse).toList();
    }
}
