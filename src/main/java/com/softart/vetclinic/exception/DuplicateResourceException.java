package com.softart.vetclinic.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String entityName;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String entityName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", entityName, fieldName, fieldValue));
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
