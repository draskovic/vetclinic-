package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateTreatmentRequest;
import com.softart.vetclinic.dto.TreatmentResponse;
import com.softart.vetclinic.dto.UpdateTreatmentRequest;
import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.mapper.TreatmentMapper;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.TaxRateRepository;
import com.softart.vetclinic.service.InventoryDeductionService;
import com.softart.vetclinic.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final TreatmentMapper treatmentMapper;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final com.softart.vetclinic.repository.ServiceRepository serviceRepository;
    private final InventoryDeductionService inventoryDeductionService;
    private final TaxRateRepository taxRateRepository;
    private final ClinicRepository clinicRepository;


    @GetMapping
    public Page<TreatmentResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return treatmentService.findAll(clinicId, pageable).map(treatmentMapper::toResponse);
    }

    @GetMapping("/{id}")
    public TreatmentResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return treatmentMapper.toResponse(treatmentService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateTreatmentRequest request) {
        var entity = treatmentMapper.toEntity(request);
        var result = treatmentService.create(entity, clinicId);

        // Auto-dodaj stavku na fakturu ako postoji
        try {
            var invoice = invoiceRepository.findByMedicalRecordIdAndDeletedFalse(entity.getMedicalRecordId());
            if (invoice.isPresent()) {
                var inv = invoice.get();
                var status = inv.getStatus();
                if (status == com.softart.vetclinic.enums.InvoiceStatus.DRAFT
                    || status == com.softart.vetclinic.enums.InvoiceStatus.ISSUED
                    || status == com.softart.vetclinic.enums.InvoiceStatus.OVERDUE) {

                    var invoiceItem = new InvoiceItem();
                    invoiceItem.setClinicId(clinicId);
                    invoiceItem.setInvoiceId(inv.getId());
                    invoiceItem.setServiceId(entity.getServiceId());
                    invoiceItem.setDescription(result.getName());
                    invoiceItem.setQuantity(BigDecimal.ONE);
                    invoiceItem.setDiscountPercent(BigDecimal.ZERO);

                    UUID serviceTaxRateId = null;
                    if (entity.getServiceId() != null) {
                        var serviceOpt = serviceRepository.findByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), clinicId);
                        if (serviceOpt.isPresent()) {
                            invoiceItem.setUnitPrice(serviceOpt.get().getPrice());
                            serviceTaxRateId = serviceOpt.get().getTaxRateId();
                        }
                    }
                    if (invoiceItem.getUnitPrice() == null) {
                        invoiceItem.setUnitPrice(BigDecimal.ZERO);
                    }

                    applyTaxRateSnapshot(invoiceItem, serviceTaxRateId, clinicId);

                    // Izračunaj lineTotal
                    var baseAmount = invoiceItem.getUnitPrice().multiply(invoiceItem.getQuantity());
                    var discountAmt = baseAmount.multiply(invoiceItem.getDiscountPercent().divide(BigDecimal.valueOf(100)));
                    var netAmount = baseAmount.subtract(discountAmt);
                    var taxAmt = netAmount.multiply(invoiceItem.getTaxRatePercent().divide(BigDecimal.valueOf(100)));
                    invoiceItem.setLineTotal(netAmount.add(taxAmt));

                    invoiceItemRepository.save(invoiceItem);
                    recalculateInvoiceTotals(clinicId, inv.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Auto-dedukcija inventara
        try {
            if (result.getServiceId() != null) {
                inventoryDeductionService.deductForTreatment(
                        clinicId, result.getServiceId(), result.getId(), entity.getVetId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return treatmentMapper.toResponse(result);
    }


    @PutMapping("/{id}")
    public TreatmentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreatmentRequest request) {
        return treatmentMapper.toResponse(
                treatmentService.update(id, clinicId, existing -> treatmentMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        var treatment = treatmentService.findById(id, clinicId);
        treatmentService.softDelete(id, clinicId);

        try {
            var invoice = invoiceRepository.findByMedicalRecordIdAndDeletedFalse(treatment.getMedicalRecordId());
            if (invoice.isPresent()) {
                var inv = invoice.get();
                var status = inv.getStatus();
                if (status == com.softart.vetclinic.enums.InvoiceStatus.DRAFT
                    || status == com.softart.vetclinic.enums.InvoiceStatus.ISSUED
                    || status == com.softart.vetclinic.enums.InvoiceStatus.OVERDUE) {

                    var items = invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, inv.getId());
                    for (var item : items) {
                        if (treatment.getServiceId() != null && treatment.getServiceId().equals(item.getServiceId())) {
                            item.setDeleted(true);
                            invoiceItemRepository.save(item);
                            break;
                        }
                    }
                    recalculateInvoiceTotals(clinicId, inv.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            inventoryDeductionService.reverseForTreatment(clinicId, id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @GetMapping("/by-medical-record/{medicalRecordId}")
    public List<TreatmentResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        return treatmentService.findByMedicalRecord(clinicId, medicalRecordId).stream()
                .map(treatmentMapper::toResponse).toList();
    }

    /**
     * Snapshot pattern: popunjava taxRateId, taxRateLabel, taxRatePercent na InvoiceItem-u.
     * Razrešavanje: 1) iz povezane Service (preferred), 2) klinika default po vatPayer.
     */
    private void applyTaxRateSnapshot(InvoiceItem entity, UUID serviceTaxRateId, UUID clinicId) {
        UUID taxRateId = serviceTaxRateId != null ? serviceTaxRateId : resolveDefaultTaxRateId(clinicId);
        TaxRate tr = taxRateRepository.findByIdAndDeletedFalse(taxRateId)
                .orElseThrow(() -> new IllegalStateException(
                        "TaxRate sa id " + taxRateId + " nije pronađen"));
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