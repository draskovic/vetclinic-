package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.repository.AppointmentRepository;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService extends AbstractCrudService<Invoice, InvoiceRepository> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InvoiceRepository invoiceRepository;
    private final OwnerRepository ownerRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicLocationRepository clinicLocationRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          OwnerRepository ownerRepository,
                          AppointmentRepository appointmentRepository,
                          ClinicLocationRepository clinicLocationRepository) {
        super(invoiceRepository);
        this.invoiceRepository = invoiceRepository;
        this.ownerRepository = ownerRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicLocationRepository = clinicLocationRepository;
    }

    @Override
    protected String getEntityName() {
        return "Invoice";
    }

    @Override
    protected Optional<Invoice> findByIdAndClinicId(UUID id, UUID clinicId) {
        return invoiceRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Invoice> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return invoiceRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return invoiceRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Invoice entity) {
        if (entity.getOwnerId() != null) {
            requireExists(
                    ownerRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getOwnerId(), entity.getClinicId()),
                    "Owner", "id", entity.getOwnerId()
            );
        }
        if (entity.getAppointmentId() != null) {
            requireExists(
                    appointmentRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getAppointmentId(), entity.getClinicId()),
                    "Appointment", "id", entity.getAppointmentId()
            );
        }
        if (entity.getLocationId() != null) {
            requireExists(
                    clinicLocationRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getLocationId(), entity.getClinicId()),
                    "ClinicLocation", "id", entity.getLocationId()
            );
        }
        if (entity.getInvoiceNumber() == null || entity.getInvoiceNumber().isBlank()) {
            entity.setInvoiceNumber(generateInvoiceNumber(entity.getClinicId()));
        }
    }

    @Transactional(readOnly = true)
    public List<Invoice> findByOwner(UUID clinicId, UUID ownerId) {
        return invoiceRepository.findByClinicIdAndOwnerIdAndDeletedFalse(clinicId, ownerId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> findByStatus(UUID clinicId, InvoiceStatus status) {
        return invoiceRepository.findByClinicIdAndStatusAndDeletedFalse(clinicId, status);
    }

    private String generateInvoiceNumber(UUID clinicId) {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String maxNumber = invoiceRepository.findMaxInvoiceNumber(clinicId);
        int nextSeq = 1;
        if (maxNumber != null && maxNumber.contains("-")) {
            String[] parts = maxNumber.split("-");
            if (parts.length == 3) {
                try {
                    nextSeq = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                    // use default 1
                }
            }
        }
        return String.format("INV-%s-%03d", dateStr, nextSeq);
    }
    
    public Page<Invoice> searchAll(UUID clinicId, String search, InvoiceStatus status, Pageable pageable) {
        if ((search == null || search.isBlank()) && status == null) {
            return findAllByClinicId(clinicId, pageable);
        }
        return invoiceRepository.searchByClinicIdAndStatus(clinicId, 
                (search == null || search.isBlank()) ? "" : search, 
                status, 
                pageable);
    }


}
