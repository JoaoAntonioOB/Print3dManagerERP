package com.print3dmanager.erp.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Taxa de sucesso das impressões")
public record PrintSuccessRateResponse(

        long concluidas,
        long falhas,
        long canceladas,
        long emAndamento,

        @Schema(description = "concluídas ÷ (concluídas + falhas) em %; null sem jobs finalizados")
        BigDecimal taxaSucesso
) {
}
