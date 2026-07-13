package com.print3dmanager.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens permitidas para CORS (chave application.cors.allowed-origins).
 */
@ConfigurationProperties(prefix = "application.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
