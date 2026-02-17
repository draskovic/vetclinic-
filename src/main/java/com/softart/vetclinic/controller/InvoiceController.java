package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateInvoiceRequest;
import com.softart.vetclinic.dto.InvoiceResponse;
import com.softart.vetclinic.dto.UpdateInvoiceRequest;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.mapper.InvoiceMapper;
import com.softart.vetclinic.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    @GetMapping
    public Page<InvoiceResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return invoiceService.findAll(clinicId, pageable).map(invoiceMapper::toResponse);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return invoiceMapper.toResponse(invoiceService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateInvoiceRequest request) {
        var entity = invoiceMapper.toEntity(request);
        return invoiceMapper.toResponse(invoiceService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        return invoiceMapper.toResponse(
                invoiceService.update(id, clinicId, existing -> invoiceMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        invoiceService.softDelete(id, clinicId);
    }

    @GetMapping("/by-owner/{ownerId}")
    public List<InvoiceResponse> getByOwner(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID ownerId) {
        return invoiceService.findByOwner(clinicId, ownerId).stream()
                .map(invoiceMapper::toResponse).toList();
    }

    @GetMapping("/by-status/{status}")
    public List<InvoiceResponse> getByStatus(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable InvoiceStatus status) {
        return invoiceService.findByStatus(clinicId, status).stream()
                .map(invoiceMapper::toResponse).toList();
    }
}
