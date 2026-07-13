package com.print3dmanager.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do JWT (chave application.security.jwt.*).
 * O secret é uma chave HMAC-SHA256 codificada em Base64;
 * as expirações são em milissegundos.
 */
@ConfigurationProperties(prefix = "application.security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {
}
