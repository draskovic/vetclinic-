package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.enums.InventoryTransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Centralni helper za primenu/reversal inventarnih transakcija na stanje LOTA.
 * (Posle splita: item više ne drži stanje — stanje = SUM(lotova). Sve se primenjuje na batch.)
 *
 * Pravila apply:   IN → +amount | OUT,EXPIRED → −amount | ADJUSTMENT → set na amount
 * Pravila reverse: IN → −amount | OUT,EXPIRED → +amount | ADJUSTMENT → no-op
 *
 * Pozivati ISKLJUČIVO posle PESSIMISTIC_WRITE lock-a na lotu.
 */
@Component
public class InventoryStockApplier {

    public BigDecimal apply(BigDecimal current, InventoryTransactionType type, BigDecimal amount) {
        return switch (type) {
            case IN -> current.add(amount);
            case OUT, EXPIRED -> current.subtract(amount);
            case ADJUSTMENT -> amount;
        };
    }

    public BigDecimal reverse(BigDecimal current, InventoryTransactionType type, BigDecimal amount) {
        return switch (type) {
            case IN -> current.subtract(amount);
            case OUT, EXPIRED -> current.add(amount);
            case ADJUSTMENT -> current;
        };
    }

    public void applyToBatch(InventoryBatch batch, InventoryTransactionType type, BigDecimal amount) {
        batch.setQuantityOnHand(apply(batch.getQuantityOnHand(), type, amount));
    }

    public void reverseOnBatch(InventoryBatch batch, InventoryTransactionType type, BigDecimal amount) {
        batch.setQuantityOnHand(reverse(batch.getQuantityOnHand(), type, amount));
    }
}