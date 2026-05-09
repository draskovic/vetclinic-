package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.CreateInvoiceFromMedicalRecordRequest;
import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.MedicalRecord;
import com.softart.vetclinic.entity.Pet;
import com.softart.vetclinic.entity.Service;
import com.softart.vetclinic.entity.Treatment;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.repository.TreatmentRepository;
import com.softart.vetclinic.util.InvoiceItemTotals;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Atomski kreira fakturu iz medical record-a (sa svim stavkama izvedenim iz treatment-a).
 * Cela operacija je @Transactional — ili sve uspeva ili ništa.
 *
 * VAŽNO: Ovaj servis radi SAMO INSERT operacije (invoice + items).
 * Recalculate (UPDATE) na invoice MORA biti pozvan iz kontrolera posle ovog servisa,
 * zbog RLS pravila iz CLAUDE.md.
 */
@org.springframework.stereotype.Service
public class InvoiceFromMedicalRecordService {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final TreatmentRepository treatmentRepository;
    private final ServiceRepository serviceRepository;
    private final TaxRateSnapshotApplier taxRateSnapshotApplier;

    public InvoiceFromMedicalRecordService(InvoiceService invoiceService,
                                            InvoiceRepository invoiceRepository,
                                            InvoiceItemRepository invoiceItemRepository,
                                            MedicalRecordRepository medicalRecordRepository,
                                            PetRepository petRepository,
                                            TreatmentRepository treatmentRepository,
                                            ServiceRepository serviceRepository,
                                            TaxRateSnapshotApplier taxRateSnapshotApplier) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.petRepository = petRepository;
        this.treatmentRepository = treatmentRepository;
        this.serviceRepository = serviceRepository;
        this.taxRateSnapshotApplier = taxRateSnapshotApplier;
    }

    @Transactional
    public Invoice createWithItems(UUID clinicId, UUID recordId,
                                    CreateInvoiceFromMedicalRecordRequest request) {
        // 1. Idempotency check — ne dozvoli duplo fakturisanje istog recorda
        invoiceRepository.findByMedicalRecordIdAndDeletedFalse(recordId).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Invoice", "medicalRecordId", recordId);
        });

        // 2. Učitaj medical record (potvrđuje pripadnost klinici)
        MedicalRecord record = medicalRecordRepository
                .findByIdAndClinicIdAndDeletedFalse(recordId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", recordId));

        // 3. Učitaj pet da dobijemo ownerId
        Pet pet = petRepository.findByIdAndClinicIdAndDeletedFalse(record.getPetId(), clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", record.getPetId()));

        // 4. Kreiraj Invoice entitet — invoiceService.create() automatski:
        //    - generiše invoiceNumber (validateForCreate u InvoiceService)
        //    - validira FK na owner/appointment/location
        //    - postavlja clinicId
        Invoice invoice = new Invoice();
        invoice.setOwnerId(pet.getOwnerId());
        invoice.setMedicalRecordId(recordId);
        invoice.setAppointmentId(record.getAppointmentId());
        invoice.setLocationId(request.locationId());
        invoice.setIssuedAt(request.issuedAt());
        invoice.setDueDate(request.dueDate());
        invoice.setStatus(request.issuedAt() != null ? InvoiceStatus.ISSUED : InvoiceStatus.DRAFT);
        invoice.setCurrency(request.currency() != null && !request.currency().isBlank()
                ? request.currency() : "RSD");
        invoice.setNote(request.note());
        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotal(BigDecimal.ZERO);

        Invoice savedInvoice = invoiceService.create(invoice, clinicId);

        // 5. Učitaj treatments
        List<Treatment> treatments = treatmentRepository
                .findByClinicIdAndMedicalRecordIdAndDeletedFalse(clinicId, recordId);

        // 6. Batch fetch services (jedan upit za sve serviceId-ove)
        List<UUID> serviceIds = treatments.stream()
                .map(Treatment::getServiceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, Service> serviceMap = serviceIds.isEmpty()
                ? Map.of()
                : serviceRepository.findAllById(serviceIds).stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getDeleted())
                                && clinicId.equals(s.getClinicId()))
                        .collect(Collectors.toMap(Service::getId, s -> s));

        // 7. Kreiraj sve InvoiceItem entitete
        List<InvoiceItem> items = new ArrayList<>();
        int sortOrder = 1;
        for (Treatment t : treatments) {
            if (t.getServiceId() == null) continue;
            Service service = serviceMap.get(t.getServiceId());
            if (service == null) continue;

            InvoiceItem item = new InvoiceItem();
            item.setClinicId(clinicId);
            item.setInvoiceId(savedInvoice.getId());
            item.setServiceId(t.getServiceId());
            item.setDescription(t.getName());
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(service.getPrice());
            item.setDiscountPercent(BigDecimal.ZERO);
            item.setSortOrder(sortOrder++);

            taxRateSnapshotApplier.apply(item, service.getTaxRateId(), clinicId);
            item.setLineTotal(InvoiceItemTotals.computeLineTotal(item));

            items.add(item);
        }

        // 8. Batch insert svih stavki — atomski
        if (!items.isEmpty()) {
            invoiceItemRepository.saveAll(items);
        }

        return savedInvoice;
    }
}