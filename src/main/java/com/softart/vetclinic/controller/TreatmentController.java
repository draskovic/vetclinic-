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
import com.softart.vetclinic.entity.MedicalRecord;
import com.softart.vetclinic.repository.MedicalRecordRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
@Slf4j
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final TreatmentMapper treatmentMapper;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final com.softart.vetclinic.repository.ServiceRepository serviceRepository;
    private final InventoryDeductionService inventoryDeductionService;
    private final InvoiceTotalsRecalculationService invoiceTotalsRecalculationService;
    private final TaxRateSnapshotApplier taxRateSnapshotApplier;
    private final MedicalRecordRepository medicalRecordRepository;

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
        // snapshot kataloške cene u tretman ako override nije poslat (karton prikazuje cenu)
        if (entity.getUnitPrice() == null && entity.getServiceId() != null) {
            serviceRepository.findByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), clinicId)
                    .ifPresent(s -> entity.setUnitPrice(s.getPrice()));
        }
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
                    invoiceItem.setTreatmentId(result.getId());   // ← NOVA: poreklo stavke = medicinski 
                    invoiceItem.setDescription(result.getName());
                    invoiceItem.setQuantity(result.getQuantity());
                    invoiceItem.setDiscountPercent(result.getDiscountPercent());

                    UUID serviceTaxRateId = null;
                    if (entity.getServiceId() != null) {
                        var serviceOpt = serviceRepository.findByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), clinicId);
                        if (serviceOpt.isPresent()) {
                            invoiceItem.setUnitPrice(result.getUnitPrice() != null ? result.getUnitPrice() : serviceOpt.get().getPrice());
                            serviceTaxRateId = serviceOpt.get().getTaxRateId();
                        }
                    }
                    if (invoiceItem.getUnitPrice() == null) {
                        invoiceItem.setUnitPrice(result.getUnitPrice() != null ? result.getUnitPrice() : BigDecimal.ZERO);
                    }

                    taxRateSnapshotApplier.apply(invoiceItem, serviceTaxRateId, clinicId);

                    // Izračunaj lineTotal
                    invoiceItem.setLineTotal(InvoiceItemTotals.computeLineTotal(invoiceItem));

                    invoiceItemRepository.save(invoiceItem);
                    invoiceTotalsRecalculationService.recalculate(clinicId, inv.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Auto-dedukcija inventara (location-aware: skida sa lokacije intervencije)
        try {
            if (result.getServiceId() != null) {
                UUID mrLoc = medicalRecordRepository
                        .findByIdAndClinicIdAndDeletedFalse(result.getMedicalRecordId(), clinicId)
                        .map(MedicalRecord::getLocationId).orElse(null);
                inventoryDeductionService.deductForTreatment(
                        clinicId, result.getServiceId(), result.getId(), entity.getVetId(), mrLoc);
            }
        } catch (Exception ex) {
            log.error("Auto-dedukcija inventara za treatment {} (clinic {}) nije uspela",
                    result.getId(), clinicId, ex);
        }

        return treatmentMapper.toResponse(result);

    }


    @PutMapping("/{id}")
    public TreatmentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreatmentRequest request) {

        // R3a: snapshot stare usluge — promena usluge menja BOM (service_inventory_item)
        var before = treatmentService.findById(id, clinicId);
        var oldServiceId = before.getServiceId();

        var result = treatmentService.update(id, clinicId,
                existing -> treatmentMapper.updateEntity(request, existing));

        if (!java.util.Objects.equals(oldServiceId, result.getServiceId())) {
            try {
                if (oldServiceId != null) {
                    inventoryDeductionService.reverseForTreatment(clinicId, id);
                }
                if (result.getServiceId() != null) {
                    UUID mrLoc = medicalRecordRepository
                            .findByIdAndClinicIdAndDeletedFalse(result.getMedicalRecordId(), clinicId)
                            .map(MedicalRecord::getLocationId).orElse(null);
                    inventoryDeductionService.deductForTreatment(
                            clinicId, result.getServiceId(), id, result.getVetId(), mrLoc);
                }
            } catch (Exception ex) {
                log.error("Korekcija inventara pri izmeni treatment {} (clinic {}) nije uspela",
                        id, clinicId, ex);
            }
        }

        // 1b sync: izmena tretmana → preslikaj na povezanu fakturnu stavku (karton = izvor istine)
        try {
            var invoice = invoiceRepository.findByMedicalRecordIdAndDeletedFalse(result.getMedicalRecordId());
            if (invoice.isPresent()) {
                var inv = invoice.get();
                var status = inv.getStatus();
                if (status == com.softart.vetclinic.enums.InvoiceStatus.DRAFT
                    || status == com.softart.vetclinic.enums.InvoiceStatus.ISSUED
                    || status == com.softart.vetclinic.enums.InvoiceStatus.OVERDUE) {

                    // efektivna cena: override sa tretmana ?? cena iz kataloga usluge
                    BigDecimal effectivePrice = result.getUnitPrice();
                    if (effectivePrice == null && result.getServiceId() != null) {
                        effectivePrice = serviceRepository.findByIdAndClinicIdAndDeletedFalse(result.getServiceId(), clinicId)
                                .map(s -> s.getPrice()).orElse(null);
                    }
                    if (effectivePrice == null) {
                        effectivePrice = BigDecimal.ZERO;
                    }

                    var items = invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, inv.getId());
                    boolean changed = false;
                    for (var li : items) {
                        if (id.equals(li.getTreatmentId())) {
                            li.setDescription(result.getName());
                            li.setQuantity(result.getQuantity());
                            li.setUnitPrice(effectivePrice);
                            li.setDiscountPercent(result.getDiscountPercent());
                            li.setLineTotal(InvoiceItemTotals.computeLineTotal(li));
                            invoiceItemRepository.save(li);
                            changed = true;
                        }
                    }
                    if (changed) {
                        invoiceTotalsRecalculationService.recalculate(clinicId, inv.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("1b sync fakturne stavke pri izmeni treatment {} (clinic {}) nije uspeo", id, clinicId, e);
        }

        return treatmentMapper.toResponse(result);
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
            log.error("Auto-reverzija inventara za treatment {} (clinic {}) nije uspela",
                    id, clinicId, ex);
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