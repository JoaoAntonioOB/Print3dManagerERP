package com.print3dmanager.erp.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Indicadores gerais do painel — contadores por status e destaques do mês")
public record DashboardResumoResponse(

        @Schema(description = "Pedidos por status (todos os status presentes, ausência = 0)")
        Map<String, Long> pedidosPorStatus,

        @Schema(description = "Orçamentos por status")
        Map<String, Long> orcamentosPorStatus,

        @Schema(description = "Impressoras por situação operacional")
        Map<String, Long> impressorasPorStatus,

        @Schema(description = "Filamentos ativos com estoque menor ou igual ao mínimo")
        long filamentosEstoqueBaixo,

        @Schema(description = "Itens de estoque ativos com quantidade menor ou igual à mínima")
        long itensEstoqueBaixo,

        long clientesAtivos,

        @Schema(description = "Jobs de impressão em andamento agora")
        long impressoesEmAndamento,

        @Schema(description = "Pedidos abertos no mês corrente")
        long pedidosMesAtual,

        @Schema(description = "Soma do valor total dos pedidos ENTREGUES no mês corrente")
        BigDecimal faturamentoMesAtual
) {
}
