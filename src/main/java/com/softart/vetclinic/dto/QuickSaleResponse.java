package com.softart.vetclinic.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Composite response Quick Sale endpoint-a — 4 dela u jednom roundtrip-u.
 *
 * Frontend može da pre-popuni React Query cache za invoice/items/payment
 * (eliminiše dodatne GET-ove), a changeAmount koristi za POS "Vrati klijentu" prikaz.
 *
 * changeAmount = max(tenderedAmount - total, 0). Nije perzistirano kao zasebna kolona;
 * trag postoji u payment.note ako je servis popunio default tekst.
 */
public record QuickSaleResponse(
        InvoiceResponse invoice,
        List<InvoiceItemResponse> items,
        PaymentResponse payment,
        BigDecimal changeAmount
) {}