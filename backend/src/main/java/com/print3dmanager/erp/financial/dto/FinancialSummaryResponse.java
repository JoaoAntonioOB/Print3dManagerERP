package com.print3dmanager.erp.financial.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Resumo financeiro do período — transações CANCELADAS não entram")
public record FinancialSummaryResponse(
        @Schema(description = "Início do período considerado (dataTransacao ≥ de)")
        LocalDate de,
        @Schema(description = "Fim do período considerado (dataTransacao ≤ ate)")
        LocalDate ate,
        BigDecimal receitasPagas,
        BigDecimal receitasPendentes,
        BigDecimal despesasPagas,
        BigDecimal despesasPendentes,
        @Schema(description = "Receitas pagas − despesas pagas")
        BigDecimal saldoRealizado,
        @Schema(description = "Receitas (pagas + pendentes) − despesas (pagas + pendentes)")
        BigDecimal saldoPrevisto
) {
}
