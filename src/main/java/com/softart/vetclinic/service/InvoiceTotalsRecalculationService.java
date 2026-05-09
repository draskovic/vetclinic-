package com.softart.vetclinic.service;

import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.util.InvoiceItemTotals;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class InvoiceTotalsRecalculationService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;

    public InvoiceTotalsRecalculationService(InvoiceItemRepository invoiceItemRepository,
                                             InvoiceRepository invoiceRepository) {
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Centralna metoda za preračunavanje totala fakture.
     * VAŽNO: Pozivati ISKLJUČIVO iz kontrolera (ne iz drugih @Transactional servisa) —
     * UPDATE na invoice tabeli ne radi iz servisa unutar iste transakcije zbog RLS.
     */
    public void recalculate(UUID clinicId, UUID invoiceId) {
        var items = invoiceItemRepository
                .findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(clinicId, invoiceId);

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