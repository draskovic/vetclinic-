package com.softart.vetclinic.controller;

import java.util.List;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.softart.vetclinic.dto.CreateInvoiceItemRequest;
import com.softart.vetclinic.dto.InvoiceItemResponse;
import com.softart.vetclinic.dto.UpdateInvoiceItemRequest;
import com.softart.vetclinic.mapper.InvoiceItemMapper;
import com.softart.vetclinic.service.InvoiceItemService;
import com.softart.vetclinic.service.InventoryDeductionService;
import com.softart.vetclinic.service.InvoiceTotalsRecalculationService;
import com.softart.vetclinic.service.TaxRateSnapshotApplier;
import com.softart.vetclinic.util.InvoiceItemTotals;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoice-items")
@RequiredArgsConstructor
@Slf4j
public class InvoiceItemController {

    private final InvoiceItemService invoiceItemService;
    private final InvoiceItemMapper invoiceItemMapper;
    private final InvoiceTotalsRecalculationService invoiceTotalsRecalculationService;
    private final TaxRateSnapshotApplier taxRateSnapshotApplier;
    private final InventoryDeductionService inventoryDeductionService;

    @GetMapping
    public Page<InvoiceItemResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return invoiceItemService.findAll(clinicId, pageable).map(invoiceItemMapper::toResponse);
    }

    @GetMapping("/{id}")
    public InvoiceItemResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return invoiceItemMapper.toResponse(invoiceItemService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceItemResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInvoiceItemRequest request) {
    	var entity = invoiceItemMapper.toEntity(request);
    	taxRateSnapshotApplier.apply(entity, request.taxRateId(), request.serviceId(), clinicId);
    	entity.setLineTotal(InvoiceItemTotals.computeLineTotal(entity));
    	var result = invoiceItemService.create(entity, clinicId);
    	// Auto-dedukcija inventara — ako je stavka vezana za artikal
    	if (result.getInventoryItemId() != null) {
    	    try {
    	        inventoryDeductionService.deductForInvoiceItem(
    	                clinicId, result.getInventoryItemId(),
    	                result.getQuantity(), result.getId(), null);
    	    } catch (Exception ex) {
    	        log.error("Auto-dedukcija inventara za invoice_item {} (clinic {}) nije uspela",
    	                result.getId(), clinicId, ex);
    	    }
    	}
    	invoiceTotalsRecalculationService.recalculate(clinicId, result.getInvoiceId());
        return invoiceItemMapper.toResponse(result);
    }

    @PutMapping("/{id}")
    public InvoiceItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceItemRequest request) {

        // R3a: snapshot PRE izmene — za korekciju inventara
        var before = invoiceItemService.findById(id, clinicId);
        var oldInventoryItemId = before.getInventoryItemId();
        var oldQuantity = before.getQuantity();

    	var result = invoiceItemService.update(id, clinicId, existing -> {
    	    invoiceItemMapper.updateEntity(request, existing);
    	    if (request.taxRateId() != null && !request.taxRateId().equals(existing.getTaxRateId())) {
    	    	taxRateSnapshotApplier.apply(existing, request.taxRateId(), existing.getServiceId(), clinicId);
    	    }
    	    existing.setLineTotal(InvoiceItemTotals.computeLineTotal(existing));
    	});

    	// R3a: ako se artikal ILI količina promenila → reverse stare dedukcije + deduktuj novu
    	boolean inventoryChanged =
    	        !java.util.Objects.equals(oldInventoryItemId, result.getInventoryItemId())
    	        || (oldQuantity != null && result.getQuantity() != null
    	            && oldQuantity.compareTo(result.getQuantity()) != 0);
    	if (inventoryChanged) {
    	    try {
    	        if (oldInventoryItemId != null) {
    	            inventoryDeductionService.reverseForInvoiceItem(clinicId, id);
    	        }
    	        if (result.getInventoryItemId() != null) {
    	            inventoryDeductionService.deductForInvoiceItem(
    	                    clinicId, result.getInventoryItemId(),
    	                    result.getQuantity(), result.getId(), null);
    	        }
    	    } catch (Exception ex) {
    	        log.error("Korekcija inventara pri izmeni invoice_item {} (clinic {}) nije uspela",
    	                id, clinicId, ex);
    	    }
    	}

    	invoiceTotalsRecalculationService.recalculate(clinicId, result.getInvoiceId());
        return invoiceItemMapper.toResponse(result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        var item = invoiceItemService.findById(id, clinicId);
        UUID invoiceId = item.getInvoiceId();
        invoiceItemService.softDelete(id, clinicId);
        // Auto-reverzija inventara — ako je stavka bila vezana za artikal
        if (item.getInventoryItemId() != null) {
            try {
                inventoryDeductionService.reverseForInvoiceItem(clinicId, id);
            } catch (Exception ex) {
                log.error("Auto-reverzija inventara za invoice_item {} (clinic {}) nije uspela",
                        id, clinicId, ex);
            }
        }
        invoiceTotalsRecalculationService.recalculate(clinicId, invoiceId);
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public List<InvoiceItemResponse> getByInvoice(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID invoiceId) {
        return invoiceItemService.findByInvoice(clinicId, invoiceId).stream()
                .map(invoiceItemMapper::toResponse).toList();
    }

}