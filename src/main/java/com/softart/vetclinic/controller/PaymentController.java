package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreatePaymentRequest;
import com.softart.vetclinic.dto.PaymentResponse;
import com.softart.vetclinic.dto.UpdatePaymentRequest;
import com.softart.vetclinic.mapper.PaymentMapper;
import com.softart.vetclinic.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

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
        return paymentMapper.toResponse(paymentService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public PaymentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentRequest request) {
        return paymentMapper.toResponse(
                paymentService.update(id, clinicId, existing -> paymentMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        paymentService.softDelete(id, clinicId);
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public List<PaymentResponse> getByInvoice(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID invoiceId) {
        return paymentService.findByInvoice(clinicId, invoiceId).stream()
                .map(paymentMapper::toResponse).toList();
    }
}
