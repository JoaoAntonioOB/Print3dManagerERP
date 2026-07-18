package com.print3dmanager.erp.order.dto;

import org.springframework.core.io.Resource;

/**
 * Arquivo de modelo 3D pronto para download: conteúdo, nome original
 * (sanitizado no upload), tamanho e content type (model/stl ou model/3mf).
 */
public record ModelFileDownload(
        Resource recurso,
        String nomeArquivo,
        long tamanhoBytes,
        String contentType
) {
}
