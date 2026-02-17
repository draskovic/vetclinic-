package com.softart.vetclinic.config.tenant;

import java.util.UUID;

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
        CONTEXT.set(clinicId);
    }

    public static UUID get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
