package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateTreatmentRequest;
import com.softart.vetclinic.dto.TreatmentResponse;
import com.softart.vetclinic.dto.UpdateTreatmentRequest;
import com.softart.vetclinic.mapper.TreatmentMapper;
import com.softart.vetclinic.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final TreatmentMapper treatmentMapper;
    private final com.softart.vetclinic.repository.InvoiceRepository invoiceRepository;
    private final com.softart.vetclinic.repository.InvoiceItemRepository invoiceItemRepository;
    private final com.softart.vetclinic.repository.ServiceRepository serviceRepository;


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
            System.out.println("invoice found: " + invoice.isPresent());
            if (invoice.isPresent()) {
                var inv = invoice.get();
                
                var status = inv.getStatus();
                System.out.println("invoice status: " + status);
                if (status == com.softart.vetclinic.enums.InvoiceStatus.DRAFT
                    || status == com.softart.vetclinic.enums.InvoiceStatus.ISSUED
                    || status == com.softart.vetclinic.enums.InvoiceStatus.OVERDUE) {

                    var invoiceItem = new com.softart.vetclinic.entity.InvoiceItem();
                    invoiceItem.setClinicId(clinicId);
                    invoiceItem.setInvoiceId(inv.getId());
                    invoiceItem.setServiceId(entity.getServiceId());
                    invoiceItem.setDescription(result.getName());
                    invoiceItem.setQuantity(java.math.BigDecimal.ONE);
                    invoiceItem.setDiscountPercent(java.math.BigDecimal.ZERO);

                    if (entity.getServiceId() != null) {
                        serviceRepository.findByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), clinicId)
                            .ifPresent(s -> {
                                invoiceItem.setUnitPrice(s.getPrice());
                                invoiceItem.setTaxRate(s.getTaxRate());
                            });
                    }
                    if (invoiceItem.getUnitPrice() == null) {
                        invoiceItem.setUnitPrice(java.math.BigDecimal.ZERO);
                    }
                    if (invoiceItem.getTaxRate() == null) {
                        invoiceItem.setTaxRate(new java.math.BigDecimal("20.00"));
                    }

                 // Izračunaj lineTotal
                    var baseAmount = invoiceItem.getUnitPrice().multiply(invoiceItem.getQuantity());
                    var discountAmt = baseAmount.multiply(invoiceItem.getDiscountPercent().divide(java.math.BigDecimal.valueOf(100)));
                    var netAmount = baseAmount.subtract(discountAmt);
                    var taxAmt = netAmount.multiply(invoiceItem.getTaxRate().divide(java.math.BigDecimal.valueOf(100)));
                    invoiceItem.setLineTotal(netAmount.add(taxAmt));

                    invoiceItemRepository.save(invoiceItem);
                   
                    recalculateInvoiceTotals(clinicId, inv.getId());
                }
            }
        } catch (Exception e) {
            // Ne blokirati kreiranje usluge ako dodavanje na fakturu ne uspe
            e.printStackTrace();
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
        
        // Dohvati treatment pre brisanja (treba nam medicalRecordId i serviceId)
        var treatment = treatmentService.findById(id, clinicId);
        
        treatmentService.softDelete(id, clinicId);
        
        // Auto-ukloni stavku sa fakture ako postoji
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
                            break; // obriši samo jednu stavku (ako ima više istih usluga)
                        }
                    }
                    recalculateInvoiceTotals(clinicId, inv.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @GetMapping("/by-medical-record/{medicalRecordId}")
    public List<TreatmentResponse> getByMedicalRecord(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID medicalRecordId) {
        return treatmentService.findByMedicalRecord(clinicId, medicalRecordId).stream()
                .map(treatmentMapper::toResponse).toList();
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
