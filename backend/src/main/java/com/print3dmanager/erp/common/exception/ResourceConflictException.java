package com.print3dmanager.erp.common.exception;

/**
 * Conflito com estado existente (ex.: e-mail duplicado) —
 * convertida em 409 pelo handler global.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
