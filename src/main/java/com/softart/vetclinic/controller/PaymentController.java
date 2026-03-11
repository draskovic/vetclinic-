package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreatePaymentRequest;
import com.softart.vetclinic.dto.PaymentResponse;
import com.softart.vetclinic.dto.UpdatePaymentRequest;
import com.softart.vetclinic.entity.Payment;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.mapper.PaymentMapper;
import com.softart.vetclinic.service.InvoiceService;
import com.softart.vetclinic.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final InvoiceService invoiceService;

    @GetMapping
    public Page<PaymentResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return paymentService.findAll(clinicId, pageable).map(paymentMapper::toResponse);
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return paymentMapper.toResponse(paymentService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreatePaymentRequest request) {
        var entity = paymentMapper.toEntity(request);
        PaymentResponse response = paymentMapper.toResponse(paymentService.create(entity, clinicId));
        updateInvoiceStatus(clinicId, request.invoiceId());
        return response;
    }

    @PutMapping("/{id}")
    public PaymentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentRequest request) {
        Payment existing = paymentService.findById(id, clinicId);
        UUID invoiceId = existing.getInvoiceId();
        PaymentResponse response = paymentMapper.toResponse(
                paymentService.update(id, clinicId, e -> paymentMapper.updateEntity(request, e)));
        updateInvoiceStatus(clinicId, invoiceId);
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        Payment existing = paymentService.findById(id, clinicId);
        UUID invoiceId = existing.getInvoiceId();
        paymentService.softDelete(id, clinicId);
        updateInvoiceStatus(clinicId, invoiceId);
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public List<PaymentResponse> getByInvoice(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID invoiceId) {
        return paymentService.findByInvoice(clinicId, invoiceId).stream()
                .map(paymentMapper::toResponse).toList();
    }

    private void updateInvoiceStatus(UUID clinicId, UUID invoiceId) {
        System.out.println(">>> 1. updateInvoiceStatus START, invoiceId=" + invoiceId);
        
        var invoice = invoiceService.findById(invoiceId, clinicId);
        System.out.println(">>> 2. Current status: " + invoice.getStatus());

        if (invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.REFUNDED) {
            return;
        }

        List<Payment> payments = paymentService.findByInvoice(clinicId, invoiceId);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println(">>> 3. Payments count: " + payments.size() + ", totalPaid: " + totalPaid + ", invoiceTotal: " + invoice.getTotal());

        final InvoiceStatus newStatus;
        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            newStatus = InvoiceStatus.ISSUED;
        } else if (totalPaid.compareTo(invoice.getTotal()) >= 0) {
            newStatus = InvoiceStatus.PAID;
        } else {
            newStatus = InvoiceStatus.PARTIALLY_PAID;
        }
        System.out.println(">>> 4. newStatus: " + newStatus + ", oldStatus: " + invoice.getStatus());

        if (newStatus != invoice.getStatus()) {
            System.out.println(">>> 5. Calling invoiceService.update()...");
            try {
                invoiceService.update(invoiceId, clinicId, inv -> inv.setStatus(newStatus));
                System.out.println(">>> 6. invoiceService.update() COMPLETED OK");
            } catch (Exception e) {
                System.out.println(">>> 6. invoiceService.update() FAILED: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> 5. Status unchanged, skipping update");
        }
    }

}
