package com.softart.vetclinic.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.softart.vetclinic.entity.Payment;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.PaymentRepository;



@Service
public class PaymentService extends AbstractCrudService<Payment, PaymentRepository> {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
  
    
    public PaymentService(PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository) {
		super(paymentRepository);
		this.paymentRepository = paymentRepository;
		this.invoiceRepository = invoiceRepository;
		
	}


    @Override
    protected String getEntityName() {
        return "Payment";
    }

    @Override
    protected Optional<Payment> findByIdAndClinicId(UUID id, UUID clinicId) {
        return paymentRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Payment> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return paymentRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return paymentRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Payment entity) {
        requireExists(
                invoiceRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getInvoiceId(), entity.getClinicId()),
                "Invoice", "id", entity.getInvoiceId()
        );
    }

    @Transactional(readOnly = true)
    public List<Payment> findByInvoice(UUID clinicId, UUID invoiceId) {
        return paymentRepository.findByClinicIdAndInvoiceIdAndDeletedFalse(clinicId, invoiceId);
    }    

}
