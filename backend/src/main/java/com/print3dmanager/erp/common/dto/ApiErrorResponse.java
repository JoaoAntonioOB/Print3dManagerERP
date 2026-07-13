package com.print3dmanager.erp.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Formato padrão de erro da API — usado pelos handlers de segurança
 * (401/403) e pelo tratamento global de exceções. O campo errors só
 * aparece em erros de validação (Jackson omite nulos).
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> errors
) {

    public record FieldValidationError(String campo, String mensagem) {
    }

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ApiErrorResponse validacao(int status, String error, String message, String path,
                                             List<FieldValidationError> errors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, errors);
    }
}
