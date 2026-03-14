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
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.service.InvoiceItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoice-items")
@RequiredArgsConstructor
public class InvoiceItemController {

    private final InvoiceItemService invoiceItemService;
    private final InvoiceItemMapper invoiceItemMapper;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;


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
        var result = invoiceItemService.create(entity, clinicId);
        recalculateInvoiceTotals(clinicId, result.getInvoiceId());
        return invoiceItemMapper.toResponse(result);

    }

    @PutMapping("/{id}")
    public InvoiceItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceItemRequest request) {
        var result = invoiceItemService.update(id, clinicId, existing -> invoiceItemMapper.updateEntity(request, existing));
        recalculateInvoiceTotals(clinicId, result.getInvoiceId());
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
        recalculateInvoiceTotals(clinicId, invoiceId);
    }


    @GetMapping("/by-invoice/{invoiceId}")
    public List<InvoiceItemResponse> getByInvoice(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID invoiceId) {
        return invoiceItemService.findByInvoice(clinicId, invoiceId).stream()
                .map(invoiceItemMapper::toResponse).toList();
    }
    private void recalculateInvoiceTotals(UUID clinicId, UUID invoiceId) {
        var items = invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, invoiceId);
        var subtotal = java.math.BigDecimal.ZERO;
        var taxAmount = java.math.BigDecimal.ZERO;
        var discountAmount = java.math.BigDecimal.ZERO;

        for (var item : items) {
            var base = item.getUnitPrice().multiply(item.getQuantity());
            var discount = base.multiply(item.getDiscountPercent().divide(java.math.BigDecimal.valueOf(100)));
            var net = base.subtract(discount);
            var tax = net.multiply(item.getTaxRate().divide(java.math.BigDecimal.valueOf(100)));

            subtotal = subtotal.add(net);
            taxAmount = taxAmount.add(tax);
            discountAmount = discountAmount.add(discount);
        }

        var invoice = invoiceRepository.findByIdAndClinicIdAndDeletedFalse(invoiceId, clinicId)
                .orElseThrow();
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotal(subtotal.add(taxAmount));
        invoiceRepository.save(invoice);
    }

}
