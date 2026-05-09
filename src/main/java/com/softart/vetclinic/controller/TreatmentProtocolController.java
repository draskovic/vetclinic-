package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.*;

import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.entity.Treatment;
import com.softart.vetclinic.mapper.TreatmentProtocolMapper;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.DiagnosisRepository;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.repository.TaxRateRepository;
import com.softart.vetclinic.service.InventoryDeductionService;
import com.softart.vetclinic.service.TreatmentProtocolItemService;
import com.softart.vetclinic.service.TreatmentProtocolService;
import com.softart.vetclinic.service.TreatmentService;
import com.softart.vetclinic.util.InvoiceItemTotals;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/treatment-protocols")
@RequiredArgsConstructor
public class TreatmentProtocolController {

    private final TreatmentProtocolService protocolService;
    private final TreatmentProtocolItemService protocolItemService;
    private final TreatmentProtocolMapper protocolMapper;
    private final TreatmentService treatmentService;
    private final DiagnosisRepository diagnosisRepository;
    private final ServiceRepository serviceRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InventoryDeductionService inventoryDeductionService;
    private final TaxRateRepository taxRateRepository;
    private final ClinicRepository clinicRepository;


    @GetMapping
    public Page<TreatmentProtocolResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "name"));
        }
        var page = protocolService.searchAll(clinicId, search, pageable);

        var diagnosisIds = page.getContent().stream()
                .map(p -> p.getDiagnosisId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> diagnosisNameMap = diagnosisIds.stream()
                .map(did -> diagnosisRepository.findByIdAndClinicIdAndDeletedFalse(did, clinicId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));

        return page.map(protocol -> {
            var response = protocolMapper.toResponse(protocol);
            String diagName = diagnosisNameMap.get(protocol.getDiagnosisId());
            return new TreatmentProtocolResponse(
                response.id(), response.name(), response.description(),
                response.diagnosisId(), diagName,
                response.active(), response.createdAt(), response.updatedAt()
            );
        });
    }

    @GetMapping("/by-diagnosis/{diagnosisId}")
    public List<TreatmentProtocolResponse> getByDiagnosis(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID diagnosisId) {
        var protocols = protocolService.findActiveByDiagnosis(clinicId, diagnosisId);
        String diagName = diagnosisRepository.findByIdAndClinicIdAndDeletedFalse(diagnosisId, clinicId)
                .map(d -> d.getName()).orElse(null);

        return protocols.stream().map(protocol -> {
            var response = protocolMapper.toResponse(protocol);
            return new TreatmentProtocolResponse(
                response.id(), response.name(), response.description(),
                response.diagnosisId(), diagName,
                response.active(), response.createdAt(), response.updatedAt()
            );
        }).toList();
    }

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TreatmentResponse> applyProtocol(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody ApplyProtocolRequest request) {

        protocolService.findById(request.protocolId(), clinicId);

        var items = protocolItemService.findByProtocol(clinicId, request.protocolId());
        if (items.isEmpty()) {
            return List.of();
        }

        var serviceIds = items.stream().map(i -> i.getServiceId()).distinct().toList();
        Map<UUID, com.softart.vetclinic.entity.Service> serviceMap = serviceIds.stream()
                .map(sid -> serviceRepository.findByIdAndClinicIdAndDeletedFalse(sid, clinicId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(s -> s.getId(), s -> s));

        List<Treatment> existingTreatments = treatmentService.findByMedicalRecord(clinicId, request.medicalRecordId());
        Set<UUID> existingServiceIds = existingTreatments.stream()
                .map(Treatment::getServiceId)
                .collect(Collectors.toSet());

        List<Treatment> createdTreatments = new ArrayList<>();
        for (var item : items) {
            var service = serviceMap.get(item.getServiceId());
            if (service == null) continue;
            if (existingServiceIds.contains(item.getServiceId())) continue;

            var treatment = new Treatment();
            treatment.setMedicalRecordId(request.medicalRecordId());
            treatment.setVetId(request.vetId());
            treatment.setServiceId(item.getServiceId());
            treatment.setName(service.getName());
            if (item.getNotes() != null) {
                treatment.setDescription(item.getNotes());
            }

            createdTreatments.add(treatmentService.create(treatment, clinicId));
        }

        // Auto-fakturisanje
        try {
            var invoice = invoiceRepository.findByMedicalRecordIdAndDeletedFalse(request.medicalRecordId());
            if (invoice.isPresent()) {
                var inv = invoice.get();
                var status = inv.getStatus();
                if (status == com.softart.vetclinic.enums.InvoiceStatus.DRAFT
                    || status == com.softart.vetclinic.enums.InvoiceStatus.ISSUED
                    || status == com.softart.vetclinic.enums.InvoiceStatus.OVERDUE) {

                    var existingInvoiceItems = invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, inv.getId());
                    Set<UUID> existingInvoiceServiceIds = existingInvoiceItems.stream()
                            .map(ii -> ii.getServiceId())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    for (int i = 0; i < items.size(); i++) {
                        var item = items.get(i);
                        var service = serviceMap.get(item.getServiceId());
                        if (service == null) continue;
                        if (existingInvoiceServiceIds.contains(item.getServiceId())) continue;

                        var invoiceItem = new InvoiceItem();
                        invoiceItem.setClinicId(clinicId);
                        invoiceItem.setInvoiceId(inv.getId());
                        invoiceItem.setServiceId(item.getServiceId());
                        invoiceItem.setDescription(service.getName());
                        invoiceItem.setQuantity(BigDecimal.valueOf(item.getQuantity()));
                        invoiceItem.setDiscountPercent(BigDecimal.ZERO);
                        invoiceItem.setUnitPrice(service.getPrice());

                        applyTaxRateSnapshot(invoiceItem, service.getTaxRateId(), clinicId);
                        invoiceItem.setLineTotal(InvoiceItemTotals.computeLineTotal(invoiceItem));

                        invoiceItemRepository.save(invoiceItem);
                    }

                    recalculateInvoiceTotals(clinicId, inv.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Auto-dedukcija inventara
        try {
            for (var t : createdTreatments) {
                if (t.getServiceId() != null) {
                    inventoryDeductionService.deductForTreatment(
                            clinicId, t.getServiceId(), t.getId(), request.vetId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return createdTreatments.stream().map(t -> {
            var service = serviceMap.get(t.getServiceId());
            return new TreatmentResponse(
                t.getId(), t.getMedicalRecordId(), t.getServiceId(),
                service != null ? service.getName() : t.getName(),
                t.getVetId(), null,
                t.getName(), t.getDescription(), t.getToothChart(), t.getResult(),
                t.getCreatedAt(), t.getUpdatedAt()
            );
        }).toList();
    }

    @GetMapping("/{id}")
    public TreatmentProtocolResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        var protocol = protocolService.findById(id, clinicId);
        var response = protocolMapper.toResponse(protocol);
        String diagName = protocol.getDiagnosisId() != null
                ? diagnosisRepository.findByIdAndClinicIdAndDeletedFalse(protocol.getDiagnosisId(), clinicId)
                    .map(d -> d.getName()).orElse(null)
                : null;
        return new TreatmentProtocolResponse(
            response.id(), response.name(), response.description(),
            response.diagnosisId(), diagName,
            response.active(), response.createdAt(), response.updatedAt()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentProtocolResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateTreatmentProtocolRequest request) {
        var entity = protocolMapper.toEntity(request);
        var result = protocolService.create(entity, clinicId);
        return protocolMapper.toResponse(result);
    }

    @PutMapping("/{id}")
    public TreatmentProtocolResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreatmentProtocolRequest request) {
        var result = protocolService.update(id, clinicId,
                existing -> protocolMapper.updateEntity(request, existing));
        var response = protocolMapper.toResponse(result);
        String diagName = result.getDiagnosisId() != null
                ? diagnosisRepository.findByIdAndClinicIdAndDeletedFalse(result.getDiagnosisId(), clinicId)
                    .map(d -> d.getName()).orElse(null)
                : null;
        return new TreatmentProtocolResponse(
            response.id(), response.name(), response.description(),
            response.diagnosisId(), diagName,
            response.active(), response.createdAt(), response.updatedAt()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        protocolService.softDelete(id, clinicId);
    }

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

            BigDecimal expectedLineTotal = InvoiceItemTotals.computeLineTotal(item);
            if (item.getLineTotal() == null || item.getLineTotal().compareTo(expectedLineTotal) != 0) {
                item.setLineTotal(expectedLineTotal);
                invoiceItemRepository.save(item);
            }
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