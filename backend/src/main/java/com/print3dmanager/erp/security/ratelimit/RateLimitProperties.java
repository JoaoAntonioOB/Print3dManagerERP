package com.print3dmanager.erp.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites de requisições por IP nas rotas abertas
 * (chave application.rate-limit.*). Janela fixa em segundos.
 */
@ConfigurationProperties(prefix = "application.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        /** Máximo de requisições por janela em /auth/** (login/refresh/logout). */
        int limiteAuth,
        /** Máximo de requisições por janela em /public/** (orçamento público). */
        int limitePublic,
        int janelaSegundos
) {
}
