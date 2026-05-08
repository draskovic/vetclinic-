package com.softart.vetclinic.controller;

import java.math.BigDecimal;
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
import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.Service;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.mapper.InvoiceItemMapper;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.repository.TaxRateRepository;
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
    private final TaxRateRepository taxRateRepository;
    private final ServiceRepository serviceRepository;
    private final ClinicRepository clinicRepository;

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
        applyTaxRateSnapshot(entity, request.taxRateId(), request.serviceId(), clinicId);
        var result = invoiceItemService.create(entity, clinicId);
        recalculateInvoiceTotals(clinicId, result.getInvoiceId());
        return invoiceItemMapper.toResponse(result);
    }

    @PutMapping("/{id}")
    public InvoiceItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceItemRequest request) {
        var result = invoiceItemService.update(id, clinicId, existing -> {
            invoiceItemMapper.updateEntity(request, existing);
            // Re-snapshot ako je klijent eksplicitno poslao novi taxRateId
            if (request.taxRateId() != null && !request.taxRateId().equals(existing.getTaxRateId())) {
                applyTaxRateSnapshot(existing, request.taxRateId(), existing.getServiceId(), clinicId);
            }
        });
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

    /**
     * Snapshot tax_rate_id (FK) + tax_rate_label + tax_rate_percent.
     * Razrešavanje:
     *  1) explicit taxRateId iz request-a
     *  2) taxRateId iz povezane Service entiteta (ako serviceId postoji)
     *  3) klinika default (Ђ ili А po vatPayer)
     */
    private void applyTaxRateSnapshot(InvoiceItem entity, UUID requestedTaxRateId,
                                       UUID serviceId, UUID clinicId) {
        UUID taxRateId = requestedTaxRateId;

        if (taxRateId == null && serviceId != null) {
            taxRateId = serviceRepository.findByIdAndClinicIdAndDeletedFalse(serviceId, clinicId)
                    .map(Service::getTaxRateId)
                    .orElse(null);
        }
        if (taxRateId == null) {
            taxRateId = resolveDefaultTaxRateId(clinicId);
        }

        UUID finalTaxRateId = taxRateId;
        TaxRate tr = taxRateRepository.findByIdAndDeletedFalse(finalTaxRateId)
                .orElseThrow(() -> new IllegalStateException(
                        "TaxRate sa id " + finalTaxRateId + " nije pronađen"));

        entity.setTaxRateId(tr.getId());
        entity.setTaxRateLabel(tr.getLabel());
        entity.setTaxRatePercent(tr.getPercent());
    }

    private UUID resolveDefaultTaxRateId(UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalStateException("Klinika ne postoji: " + clinicId));
        String label = Boolean.TRUE.equals(clinic.getVatPayer()) ? "Ђ" : "А";
        return taxRateRepository.findByCountryCodeAndLabel("RS", label)
                .orElseThrow(() -> new IllegalStateException(
                        "Default TaxRate '" + label + "' (RS) nije pronađen u šifarniku"))
                .getId();
    }

    private void recalculateInvoiceTotals(UUID clinicId, UUID invoiceId) {
        var items = invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, invoiceId);
        var subtotal = BigDecimal.ZERO;
        var taxAmount = BigDecimal.ZERO;
        var discountAmount = BigDecimal.ZERO;

        for (var item : items) {
            var base = item.getUnitPrice().multiply(item.getQuantity());
            var discount = base.multiply(item.getDiscountPercent().divide(BigDecimal.valueOf(100)));
            var net = base.subtract(discount);
            var tax = net.multiply(item.getTaxRatePercent().divide(BigDecimal.valueOf(100)));

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