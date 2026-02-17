package com.softart.vetclinic.exception;

import java.util.UUID;

public class ClinicMismatchException extends RuntimeException {

    public ClinicMismatchException(String entityName, UUID entityId, UUID expectedClinicId) {
        super(String.format("%s with id '%s' does not belong to clinic '%s'",
                entityName, entityId, expectedClinicId));
    }
}
