package com.softart.vetclinic.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.softart.vetclinic.dto.QuickSaleRequest;
import com.softart.vetclinic.dto.QuickSaleResponse;
import com.softart.vetclinic.mapper.InvoiceItemMapper;
import com.softart.vetclinic.mapper.InvoiceMapper;
import com.softart.vetclinic.mapper.PaymentMapper;
import com.softart.vetclinic.service.QuickSaleService;
import com.softart.vetclinic.service.QuickSaleService.QuickSaleResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Quick Sale (POS) endpoint — atomska prodaja artikla/usluge bez vezivanja za medical record.
 *
 * Jedan POST → kreira:
 *   - Invoice (sa već popunjenim totals, status PAID/PARTIALLY_PAID)
 *   - InvoiceItem[] (snapshot PDV-a)
 *   - Payment (amount = min(tendered, total), note = default "Primljeno X, kusur Y")
 *   - Auto-OUT inventory transakcije za stavke sa inventoryItemId (FIFO za batch-tracked)
 *
 * Sve u JEDNOJ DB transakciji (QuickSaleService.createSale je @Transactional).
 * Kontroler radi samo mapiranje entiteta u DTO response.
 */
@RestController
@RequestMapping("/api/quick-sale")
@RequiredArgsConstructor
public class QuickSaleController {

    private final QuickSaleService quickSaleService;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceItemMapper invoiceItemMapper;
    private final PaymentMapper paymentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuickSaleResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody QuickSaleRequest request) {

        QuickSaleResult result = quickSaleService.createSale(clinicId, request);

        return new QuickSaleResponse(
                invoiceMapper.toResponse(result.invoice()),
                result.items().stream().map(invoiceItemMapper::toResponse).toList(),
                paymentMapper.toResponse(result.payment()),
                result.changeAmount()
        );
    }
}