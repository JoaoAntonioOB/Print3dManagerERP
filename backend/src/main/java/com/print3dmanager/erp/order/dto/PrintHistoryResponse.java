package com.print3dmanager.erp.order.dto;

import com.print3dmanager.erp.order.model.PrintStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Registro de um job de impressão")
public record PrintHistoryResponse(
        Long id,
        Long impressoraId,
        String impressoraNome,
        Long filamentoId,
        String filamentoNome,
        Long itemPedidoId,
        String nomePeca,
        @Schema(description = "Número do pedido do item, quando houver")
        String pedidoNumero,
        Long usuarioId,
        String usuarioNome,
        PrintStatus status,
        Instant iniciadoEm,
        Instant finalizadoEm,
        Integer tempoTotalMinutos,
        BigDecimal pesoUtilizadoG,
        BigDecimal consumoEnergiaKwh,
        @Schema(description = "Custo real: filamento + energia + (hora máquina + desgaste), "
                + "calculado com a configuração efetiva da impressora quando disponível")
        BigDecimal custoTotal,
        String motivoFalha,
        String observacoes,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
