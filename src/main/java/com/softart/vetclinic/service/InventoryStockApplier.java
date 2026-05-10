package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.InventoryBatch;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.enums.InventoryTransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Centralni helper za primenu/reversal inventarnih transakcija na stanje (batch ili item).
 *
 * Pravila:
 * - IN              → current + amount
 * - OUT, EXPIRED    → current - amount
 * - ADJUSTMENT      → amount (postavi na zadatu vrednost, NE add/subtract)
 *
 * Reversal (poništavanje stare tx pri update-u ili storno-u):
 * - IN              → current - amount   (poništi dodavanje)
 * - OUT, EXPIRED    → current + amount   (poništi skidanje)
 * - ADJUSTMENT      → no-op              (ne može se poništiti deterministički bez snapshot-a
 *                                          prethodne vrednosti — odgovara postojećem semantičkom
 *                                          ponašanju koda pre refaktora)
 *
 * Pozivati ISKLJUČIVO posle pribavljanja entiteta sa PESSIMISTIC_WRITE lock-om
 * (`findByIdAndClinicIdAndDeletedFalseForUpdate`) — helper ne brine o concurrency-ju.
 */
@Component
public class InventoryStockApplier {

    /**
     * Vraća novu vrednost stanja posle primene transakcije. Bez side-effect-a (čista funkcija).
     */
    public BigDecimal apply(BigDecimal current, InventoryTransactionType type, BigDecimal amount) {
        return switch (type) {
            case IN -> current.add(amount);
            case OUT, EXPIRED -> current.subtract(amount);
            case ADJUSTMENT -> amount;
        };
    }

    /**
     * Vraća novu vrednost stanja posle poništavanja transakcije. Bez side-effect-a.
     * ADJUSTMENT vraća current bez promene (vidi class-level Javadoc).
     */
    public BigDecimal reverse(BigDecimal current, InventoryTransactionType type, BigDecimal amount) {
        return switch (type) {
            case IN -> current.subtract(amount);
            case OUT, EXPIRED -> current.add(amount);
            case ADJUSTMENT -> current;
        };
    }

    // -----------------------------------------------------------------
    // Convenience metode — direktno na entitetima (popunjavaju setter)
    // -----------------------------------------------------------------

    public void applyToBatch(InventoryBatch batch, InventoryTransactionType type, BigDecimal amount) {
        batch.setQuantityOnHand(apply(batch.getQuantityOnHand(), type, amount));
    }

    public void applyToItem(InventoryItem item, InventoryTransactionType type, BigDecimal amount) {
        item.setQuantityOnHand(apply(item.getQuantityOnHand(), type, amount));
    }

    public void reverseOnBatch(InventoryBatch batch, InventoryTransactionType type, BigDecimal amount) {
        batch.setQuantityOnHand(reverse(batch.getQuantityOnHand(), type, amount));
    }

    public void reverseOnItem(InventoryItem item, InventoryTransactionType type, BigDecimal amount) {
        item.setQuantityOnHand(reverse(item.getQuantityOnHand(), type, amount));
    }
}