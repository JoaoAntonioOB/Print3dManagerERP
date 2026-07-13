package com.print3dmanager.erp.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Item (peça) de um pedido")
public record OrderItemResponse(
        Long id,
        Long filamentoId,
        String filamentoNome,
        String nomePeca,
        String descricao,
        Integer quantidade,
        BigDecimal pesoEstimadoG,
        Integer tempoImpressaoMinutos,
        BigDecimal precoUnitario,
        @Schema(description = "Quantidade × preço unitário")
        BigDecimal subtotal,
        @Schema(description = "Caminho do arquivo STL/3MF (upload em etapa futura)")
        String arquivoModelo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
