package com.softart.vetclinic.exception;

public class DuplicateResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;
	private final String entityName;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateResourceException(String entityName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", entityName, fieldName, fieldValue));
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * Poruka namenjena krajnjem korisniku (prikazuje se direktno u UI-ju).
     * Koristiti kada tehnicki opis (entitet + polje + UUID) ne pomaze korisniku.
     */
    public DuplicateResourceException(String userMessage) {
        super(userMessage);
        this.entityName = null;
        this.fieldName = null;
        this.fieldValue = null;
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
