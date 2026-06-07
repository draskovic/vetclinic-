package com.softart.vetclinic.controller;

import java.util.List;
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

import com.softart.vetclinic.dto.CreateInventoryTransactionRequest;
import com.softart.vetclinic.dto.InventoryTransactionResponse;
import com.softart.vetclinic.dto.UpdateInventoryTransactionRequest;
import com.softart.vetclinic.entity.InventoryTransaction;
import com.softart.vetclinic.enums.InventoryTransactionType;
import com.softart.vetclinic.mapper.InventoryTransactionMapper;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.ProductRepository;
import com.softart.vetclinic.service.InventoryTransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService inventoryTransactionService;
    private final InventoryTransactionMapper inventoryTransactionMapper;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;

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
        var page = inventoryTransactionService.searchAll(clinicId, search, type, inventoryItemId, pageable);
        List<InventoryTransactionResponse> enriched = toEnrichedResponses(page.getContent(), clinicId);
        return new org.springframework.data.domain.PageImpl<>(enriched, pageable, page.getTotalElements());
    }


    @GetMapping("/{id}")
    public InventoryTransactionResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        InventoryTransaction tx = inventoryTransactionService.findById(id, clinicId);
        return toEnrichedResponses(List.of(tx), clinicId).get(0);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTransactionResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInventoryTransactionRequest request) {
        var entity = inventoryTransactionMapper.toEntity(request);
        entity.setBatchId(request.batchId());
        InventoryTransaction saved = inventoryTransactionService.create(entity, clinicId);
        return toEnrichedResponses(List.of(saved), clinicId).get(0);
    }

    @PutMapping("/{id}")
    public InventoryTransactionResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryTransactionRequest request) {
        return inventoryTransactionMapper.toResponse(
                inventoryTransactionService.update(id, clinicId, existing -> {
                inventoryTransactionMapper.updateEntity(request, existing);
                existing.setBatchId(request.batchId());
                }));
        
        
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
    	return toEnrichedResponses(inventoryTransactionService.findByItem(clinicId, inventoryItemId), clinicId);
    }
    
    /** Batch-fetch status lotova (batchDeleted/batchNumber) + ime artikla (product.name). */
    private List<InventoryTransactionResponse> toEnrichedResponses(List<InventoryTransaction> txs, UUID clinicId) {
        Set<UUID> batchIds = txs.stream()
                .map(InventoryTransaction::getBatchId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> activeBatchIds = batchIds.isEmpty()
                ? Set.of()
                : inventoryBatchRepository.findActiveIdsByIdInAndClinicId(batchIds, clinicId);

        java.util.Map<UUID, String> batchNumbers = batchIds.isEmpty()
                ? java.util.Map.of()
                : inventoryBatchRepository.findBatchNumbersByIdInAndClinicId(batchIds, clinicId)
                    .stream()
                    .collect(Collectors.toMap(
                            row -> (UUID) row[0],
                            row -> (String) row[1]
                    ));

        java.util.Map<UUID, String> itemNames = resolveItemNames(txs, clinicId);

        return txs.stream()
                .map(tx -> enrich(tx, activeBatchIds, batchNumbers, itemNames))
                .toList();
    }

    /** Artikal → product.name (1–2 batch upita, bez lazy navigacije). */
    private java.util.Map<UUID, String> resolveItemNames(List<InventoryTransaction> txs, UUID clinicId) {
        Set<UUID> itemIds = txs.stream()
                .map(InventoryTransaction::getInventoryItemId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) return java.util.Map.of();

        java.util.Map<UUID, UUID> itemToProduct = new java.util.HashMap<>();
        for (UUID itemId : itemIds) {
            inventoryItemRepository.findByIdAndClinicIdAndDeletedFalse(itemId, clinicId)
                    .ifPresent(i -> itemToProduct.put(itemId, i.getProductId()));
        }
        Set<UUID> productIds = new java.util.HashSet<>(itemToProduct.values());
        java.util.Map<UUID, String> productNames = productIds.isEmpty()
                ? java.util.Map.of()
                : productRepository.findAllById(productIds).stream()
                        .collect(Collectors.toMap(
                                com.softart.vetclinic.entity.Product::getId,
                                com.softart.vetclinic.entity.Product::getName));

        java.util.Map<UUID, String> itemNames = new java.util.HashMap<>();
        itemToProduct.forEach((itemId, prodId) -> itemNames.put(itemId, productNames.get(prodId)));
        return itemNames;
    }

    private InventoryTransactionResponse enrich(InventoryTransaction tx, Set<UUID> activeBatchIds,
            java.util.Map<UUID, String> batchNumbers, java.util.Map<UUID, String> itemNames) {
        InventoryTransactionResponse base = inventoryTransactionMapper.toResponse(tx);
        boolean batchDeleted = tx.getBatchId() != null && !activeBatchIds.contains(tx.getBatchId());
        String batchNumber = tx.getBatchId() != null ? batchNumbers.get(tx.getBatchId()) : null;
        String inventoryItemName = tx.getInventoryItemId() != null
                ? itemNames.get(tx.getInventoryItemId()) : null;
        return new InventoryTransactionResponse(
                base.id(), base.inventoryItemId(), inventoryItemName, base.batchId(),
                base.type(), base.quantity(), base.referenceType(), base.referenceId(),
                base.performedBy(), base.performedByName(), base.note(), base.reason(),
                base.reversalOfTransactionId(), base.reversed(), batchNumber, batchDeleted,
                base.createdAt(), base.updatedAt()
        );
    }
    
    @PostMapping("/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryTransactionResponse reverse(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        InventoryTransaction saved = inventoryTransactionService.reverse(id, clinicId);
        return toEnrichedResponses(List.of(saved), clinicId).get(0);
    }
}
