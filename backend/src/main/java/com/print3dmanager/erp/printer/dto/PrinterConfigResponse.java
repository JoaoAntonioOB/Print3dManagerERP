package com.print3dmanager.erp.printer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Configuração de custos (global ou de uma impressora específica)")
public record PrinterConfigResponse(
        Long id,
        @Schema(description = "Id da impressora dona da configuração; null = configuração global")
        Long impressoraId,
        BigDecimal valorKwh,
        BigDecimal valorHoraMaquina,
        BigDecimal custoDesgasteHora,
        BigDecimal markupPadrao,
        @Schema(description = "Origem da configuração retornada: PROPRIA ou GLOBAL",
                allowableValues = {"PROPRIA", "GLOBAL"})
        String origem,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
