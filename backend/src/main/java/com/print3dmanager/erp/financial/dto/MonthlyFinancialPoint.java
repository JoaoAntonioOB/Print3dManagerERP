package com.print3dmanager.erp.financial.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Movimento financeiro realizado (transações PAGAS) de um mês")
public record MonthlyFinancialPoint(
        @Schema(description = "Mês no formato YYYY-MM", example = "2026-07")
        String mes,
        BigDecimal receitas,
        BigDecimal despesas,
        @Schema(description = "Receitas − despesas do mês")
        BigDecimal saldo
) {
}
