package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateInvoiceItemRequest;
import com.softart.vetclinic.dto.InvoiceItemResponse;
import com.softart.vetclinic.dto.UpdateInvoiceItemRequest;
import com.softart.vetclinic.mapper.InvoiceItemMapper;
import com.softart.vetclinic.service.InvoiceItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoice-items")
@RequiredArgsConstructor
public class InvoiceItemController {

    private final InvoiceItemService invoiceItemService;
    private final InvoiceItemMapper invoiceItemMapper;

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
        return invoiceItemMapper.toResponse(invoiceItemService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public InvoiceItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceItemRequest request) {
        return invoiceItemMapper.toResponse(
                invoiceItemService.update(id, clinicId, existing -> invoiceItemMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        invoiceItemService.softDelete(id, clinicId);
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public List<InvoiceItemResponse> getByInvoice(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID invoiceId) {
        return invoiceItemService.findByInvoice(clinicId, invoiceId).stream()
                .map(invoiceItemMapper::toResponse).toList();
    }
}
