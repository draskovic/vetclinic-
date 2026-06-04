package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Quick Sale (POS) — atomska prodaja artikla/usluge bez vezivanja za medical record.
 *
 * Kupac:
 *  - ownerId i walkInCustomerName su OPCIONI (anonimni kupac kad su oba null).
 *  - Ako je ownerId postavljen, servis validira pripadnost klinici.
 *
 * Plaćanje (Smart A — tendered/change model):
 *  - tenderedAmount = koliko je kupac dao u kasu (klijent salje).
 *  - Server računa:
 *      payment.amount = min(tenderedAmount, total)
 *      changeAmount   = max(tenderedAmount - total, 0)        (vraća se u response)
 *      status         = tenderedAmount >= total ? PAID : PARTIALLY_PAID
 *  - Ako changeAmount > 0, servis popunjava payment.note default tekstom
 *    "Primljeno: X RSD, vraćen kusur: Y RSD" (može se prepisati kroz paymentNote).
 *
 * Audit:
 *  - vetId opcioni — postavlja se kao performed_by na auto-OUT inventarnim tx-ovima.
 */
public record QuickSaleRequest(
        UUID ownerId,
        String walkInCustomerName,
        UUID locationId,
        UUID vetId,
        OffsetDateTime issuedAt,
        String currency,
        String note,

        @NotEmpty @Valid List<QuickSaleLineRequest> lines,

        @NotNull PaymentMethod paymentMethod,
        @NotNull BigDecimal tenderedAmount,
        OffsetDateTime paidAt,
        String paymentReferenceNumber,
        String paymentNote
) {}