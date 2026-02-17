package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceItemService extends AbstractCrudService<InvoiceItem, InvoiceItemRepository> {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final ServiceRepository serviceRepository;

    public InvoiceItemService(InvoiceItemRepository invoiceItemRepository,
                              InvoiceRepository invoiceRepository,
                              ServiceRepository serviceRepository) {
        super(invoiceItemRepository);
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    protected String getEntityName() {
        return "InvoiceItem";
    }

    @Override
    protected Optional<InvoiceItem> findByIdAndClinicId(UUID id, UUID clinicId) {
        return invoiceItemRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<InvoiceItem> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return invoiceItemRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return invoiceItemRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(InvoiceItem entity) {
        requireExists(
                invoiceRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getInvoiceId(), entity.getClinicId()),
                "Invoice", "id", entity.getInvoiceId()
        );
        if (entity.getServiceId() != null) {
            requireExists(
                    serviceRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getServiceId(), entity.getClinicId()),
                    "Service", "id", entity.getServiceId()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<InvoiceItem> findByInvoice(UUID clinicId, UUID invoiceId) {
        return invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, invoiceId);
    }
}
