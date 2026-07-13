package com.print3dmanager.erp.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Formato padrão de listagem paginada da API — envelope estável,
 * independente da serialização interna do Page do Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
