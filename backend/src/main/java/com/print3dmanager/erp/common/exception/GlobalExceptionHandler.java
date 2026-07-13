package com.print3dmanager.erp.common.exception;

import com.print3dmanager.erp.common.dto.ApiErrorResponse;
import com.print3dmanager.erp.common.dto.ApiErrorResponse.FieldValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Converte exceções em respostas JSON no formato padrão da API
 * (mesmo formato dos handlers 401/403 do Spring Security).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidacao(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        List<FieldValidationError> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::paraErroDeCampo)
                .toList();

        return construir(HttpStatus.BAD_REQUEST, "Erro de validação nos campos enviados.",
                request, erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleCorpoIlegivel(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou JSON malformado.", request, null);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleCredenciaisInvalidas(BadCredentialsException ex,
                                                                       HttpServletRequest request) {
        // Mensagem genérica em login para não revelar se o e-mail existe;
        // no refresh a mensagem específica da exceção é mais útil.
        String mensagem = request.getRequestURI().endsWith("/auth/login")
                ? "E-mail ou senha inválidos."
                : ex.getMessage();
        return construir(HttpStatus.UNAUTHORIZED, mensagem, request, null);
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiErrorResponse> handleUsuarioInativo(AuthenticationException ex,
                                                                 HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "Usuário inativo ou bloqueado.", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAutenticacao(AuthenticationException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "Falha na autenticação.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAcessoNegado(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN,
                "Acesso negado: você não tem permissão para acessar este recurso.", request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNaoEncontrado(ResourceNotFoundException ex,
                                                                HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRotaInexistente(NoResourceFoundException ex,
                                                                  HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleRegraDeNegocio(BusinessException ex,
                                                                 HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleErroInterno(Exception ex,
                                                              HttpServletRequest request) {
        log.error("Erro interno não tratado em {} {}", request.getMethod(),
                request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Tente novamente mais tarde.", request, null);
    }

    private FieldValidationError paraErroDeCampo(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> construir(HttpStatus status, String mensagem,
                                                       HttpServletRequest request,
                                                       List<FieldValidationError> erros) {
        ApiErrorResponse body = erros == null
                ? ApiErrorResponse.of(status.value(), status.getReasonPhrase(), mensagem,
                        request.getRequestURI())
                : ApiErrorResponse.validacao(status.value(), status.getReasonPhrase(), mensagem,
                        request.getRequestURI(), erros);
        return ResponseEntity.status(status).body(body);
    }
}
