package com.softart.vetclinic.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientInventoryException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InsufficientInventoryException(String message) {
        super(message);
    }
}