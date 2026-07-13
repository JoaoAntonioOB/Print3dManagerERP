package com.print3dmanager.erp.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Dados de um item de estoque (insumo geral)")
public record InventoryItemResponse(
        Long id,
        String nome,
        String descricao,
        String categoria,
        BigDecimal quantidade,
        String unidadeMedida,
        BigDecimal quantidadeMinima,
        @Schema(description = "true quando a quantidade em estoque é menor ou igual à mínima")
        boolean estoqueBaixo,
        BigDecimal custoUnitario,
        String localizacao,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
