package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateInvoiceFromMedicalRecordRequest;
import com.softart.vetclinic.dto.CreateInvoiceRequest;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.softart.vetclinic.dto.InvoiceResponse;
import com.softart.vetclinic.dto.InvoiceWithItemsResponse;
import com.softart.vetclinic.dto.UpdateInvoiceRequest;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.mapper.InvoiceItemMapper;
import com.softart.vetclinic.mapper.InvoiceMapper;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.service.InvoiceService;
import com.softart.vetclinic.service.InvoiceTotalsRecalculationService;
import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.entity.InvoiceItem;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.softart.vetclinic.service.InvoiceFromMedicalRecordService;
import com.softart.vetclinic.service.InvoicePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoicePdfService invoicePdfService;
    private final InvoiceFromMedicalRecordService invoiceFromMedicalRecordService;
    private final InvoiceTotalsRecalculationService invoiceTotalsRecalculationService;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceItemMapper invoiceItemMapper;


    @GetMapping
    public Page<InvoiceResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InvoiceStatus status,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return invoiceService.searchAll(clinicId, search, status, pageable).map(invoiceMapper::toResponse);
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
    
    @PostMapping("/from-medical-record/{recordId}")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceWithItemsResponse createFromMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID recordId,
            @Valid @RequestBody CreateInvoiceFromMedicalRecordRequest request) {

        // 1. Atomski INSERT: invoice + sve stavke (jedna transakcija u servisu)
        Invoice invoice = invoiceFromMedicalRecordService.createWithItems(clinicId, recordId, request);

        // 2. Recalculate (UPDATE) iz kontrolera — sveža konekcija, RLS-safe
        invoiceTotalsRecalculationService.recalculate(clinicId, invoice.getId());

        // 3. Vrati fresh stanje (totals posle recalculate, items sortirani)
        Invoice freshInvoice = invoiceService.findById(invoice.getId(), clinicId);
        List<InvoiceItem> freshItems = invoiceItemRepository
                .findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, invoice.getId());

        return new InvoiceWithItemsResponse(
                invoiceMapper.toResponse(freshInvoice),
                freshItems.stream().map(invoiceItemMapper::toResponse).toList()
        );
    }
    
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {

        byte[] pdfBytes = invoicePdfService.generatePdf(id, clinicId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "faktura-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }


    @PutMapping("/{id}")
    public InvoiceResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        return invoiceMapper.toResponse(
                invoiceService.update(id, clinicId, existing -> {
                    // Optimistic locking — backwards-kompatibilno: ako klijent ne šalje version, preskačemo check
                    if (request.version() != null && !request.version().equals(existing.getVersion())) {
                        throw new ObjectOptimisticLockingFailureException(
                                Invoice.class.getName(), id);
                    }
                    invoiceMapper.updateEntity(request, existing);
                }));
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
    
    @GetMapping("/by-medical-record/{medicalRecordId}")
    public ResponseEntity<InvoiceResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        // Vraća 200 OK sa null telom kada intervencija nema fakturu — to nije "not found"
        // nego validan business state ("još nije fakturisano"). Frontend proverava na null.
        return invoiceRepository.findByMedicalRecordIdAndDeletedFalse(medicalRecordId)
                .map(invoice -> ResponseEntity.ok(invoiceMapper.toResponse(invoiceService.findById(invoice.getId(), clinicId))))
                .orElse(ResponseEntity.ok(null));
    }


}
