package com.print3dmanager.erp.common.exception;

/**
 * Violação de regra de negócio — convertida em 400 pelo handler global.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
