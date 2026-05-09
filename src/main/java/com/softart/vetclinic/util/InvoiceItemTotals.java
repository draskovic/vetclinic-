package com.softart.vetclinic.util;

import com.softart.vetclinic.entity.InvoiceItem;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Backend = single source of truth za line_total stavke fakture.
 * Klijentova vrednost se ignoriše — uvek se računa iz snapshot polja.
 *
 * Formula:
 *   base     = unit_price * quantity
 *   discount = base * (discount_percent / 100)
 *   net      = base - discount
 *   tax      = net * (tax_rate_percent / 100)
 *   line     = net + tax
 */
public final class InvoiceItemTotals {

    private InvoiceItemTotals() {}

    public static BigDecimal computeLineTotal(InvoiceItem item) {
        BigDecimal base = item.getUnitPrice().multiply(item.getQuantity());
        BigDecimal discount = base
                .multiply(item.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal net = base.subtract(discount);
        BigDecimal tax = net
                .multiply(item.getTaxRatePercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return net.add(tax).setScale(2, RoundingMode.HALF_UP);
    }
}