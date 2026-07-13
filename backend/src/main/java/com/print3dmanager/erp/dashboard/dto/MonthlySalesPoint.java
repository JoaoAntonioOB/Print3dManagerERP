package com.print3dmanager.erp.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Ponto mensal de vendas: pedidos abertos e faturamento entregue no mês")
public record MonthlySalesPoint(

        @Schema(description = "Mês no formato YYYY-MM", example = "2026-07")
        String mes,

        @Schema(description = "Pedidos abertos no mês")
        long pedidos,

        @Schema(description = "Valor total dos pedidos ENTREGUES no mês (pela data de entrega)")
        BigDecimal faturamento
) {
}
