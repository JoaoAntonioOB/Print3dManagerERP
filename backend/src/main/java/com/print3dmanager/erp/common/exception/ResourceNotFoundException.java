package com.print3dmanager.erp.common.exception;

/**
 * Recurso não encontrado — convertida em 404 pelo handler global.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String recurso, Object id) {
        super("%s não encontrado(a) com o identificador: %s".formatted(recurso, id));
    }
}
