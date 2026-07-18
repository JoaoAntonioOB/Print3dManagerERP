package com.print3dmanager.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Armazenamento de uploads em disco (chave application.storage.*).
 * Em Docker o diretório é o volume {@code backend-uploads}.
 */
@ConfigurationProperties(prefix = "application.storage")
public record StorageProperties(
        String uploadDir
) {
}
