package com.print3dmanager.erp.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.print3dmanager.erp.common.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limit por IP nas rotas abertas ao público — as únicas onde um
 * atacante age sem credencial: {@code /auth/**} (força bruta de login e
 * enumeração de refresh tokens) e {@code /public/**} (link de orçamento).
 * Excedeu o limite da janela → 429 JSON com {@code Retry-After}.
 * Preflight CORS (OPTIONS) não conta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Integer limite = limitePara(request);
        if (limite == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String chave = grupoDe(request) + ":" + request.getRemoteAddr();
        if (rateLimitService.permitir(chave, limite, properties.janelaSegundos())) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = rateLimitService.segundosParaLiberar(
                chave, properties.janelaSegundos());
        log.warn("Rate limit excedido para {} em {} {}", request.getRemoteAddr(),
                request.getMethod(), request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Muitas requisições em sequência. Tente novamente em %d segundos."
                        .formatted(retryAfter),
                request.getRequestURI()));
    }

    /** Limite aplicável à rota, ou null quando a rota não é limitada. */
    private Integer limitePara(HttpServletRequest request) {
        if (!properties.enabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        return switch (grupoDe(request)) {
            case "auth" -> properties.limiteAuth();
            case "public" -> properties.limitePublic();
            default -> null;
        };
    }

    /** Grupo da rota sem o context path (/api): "auth", "public" ou "". */
    private String grupoDe(HttpServletRequest request) {
        String caminho = request.getRequestURI()
                .substring(request.getContextPath().length());
        if (caminho.startsWith("/auth/")) {
            return "auth";
        }
        if (caminho.startsWith("/public/")) {
            return "public";
        }
        return "";
    }
}
