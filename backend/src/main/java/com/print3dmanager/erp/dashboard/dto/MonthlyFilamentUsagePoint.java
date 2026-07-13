package com.print3dmanager.erp.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Consumo mensal de filamento nas impressões finalizadas")
public record MonthlyFilamentUsagePoint(

        @Schema(description = "Mês no formato YYYY-MM", example = "2026-07")
        String mes,

        @Schema(description = "Gramas consumidas no mês (inclui desperdício de falhas)")
        BigDecimal pesoG
) {
}
