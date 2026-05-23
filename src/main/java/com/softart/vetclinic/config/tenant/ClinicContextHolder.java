package com.softart.vetclinic.config.tenant;

import java.util.UUID;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * ThreadLocal holder for the current clinic ID.
 * Used to propagate tenant context to the database layer (RLS).
 * Independent of Spring Security — can be set from JWT filter, AuthService, or DataSeeder.
 */
public final class ClinicContextHolder {

    private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();

    private ClinicContextHolder() {
        // utility class
    }

    public static void set(UUID clinicId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            UUID current = CONTEXT.get();
            
            // Slučaj 1: Pokušaj promene klinike unutar transakcije
            if (current != null && !current.equals(clinicId)) {
                throw new IllegalStateException(
                    "Attempt to CHANGE tenant context inside an active transaction. " +
                    "Current: " + current + ", attempted: " + clinicId);
            }
            
            // Slučaj 2: Pokušaj inicijalizacije konteksta nakon što je transakcija već počela
            if (current == null && clinicId != null) {
                throw new IllegalStateException(
                    "Attempt to INITIALIZE tenant context inside an active transaction. " +
                    "Context must be set BEFORE the transaction starts so that DataSource can apply RLS. " +
                    "Attempted: " + clinicId);
            }
        }
        CONTEXT.set(clinicId);
    }

    public static UUID get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
