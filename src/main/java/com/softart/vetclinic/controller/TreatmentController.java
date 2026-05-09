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

import com.softart.vetclinic.dto.CreateTreatmentRequest;
import com.softart.vetclinic.dto.TreatmentResponse;
import com.softart.vetclinic.dto.UpdateTreatmentRequest;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.mapper.TreatmentMapper;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.service.InventoryDeductionService;
import com.softart.vetclinic.service.InvoiceTotalsRecalculationService;
import com.softart.vetclinic.service.TaxRateSnapshotApplier;
import com.softart.vetclinic.service.TreatmentService;
import com.softart.vetclinic.util.InvoiceItemTotals;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
    private final InvoiceTotalsRecalculationService invoiceTotalsRecalculationService;
    private final TaxRateSnapshotApplier taxRateSnapshotApplier;

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

                    taxRateSnapshotApplier.apply(invoiceItem, serviceTaxRateId, clinicId);

                    // Izračunaj lineTotal
                    taxRateSnapshotApplier.apply(invoiceItem, serviceTaxRateId, clinicId);
                    invoiceItem.setLineTotal(InvoiceItemTotals.computeLineTotal(invoiceItem));

                    invoiceItemRepository.save(invoiceItem);
                    invoiceTotalsRecalculationService.recalculate(clinicId, inv.getId());
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
                    invoiceTotalsRecalculationService.recalculate(clinicId, inv.getId());
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

}