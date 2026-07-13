package com.print3dmanager.erp.common.dto;

import java.time.Instant;

/**
 * Formato padrão de erro da API — usado pelos handlers de segurança
 * (401/403) e pelo tratamento global de exceções.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path);
    }
}
