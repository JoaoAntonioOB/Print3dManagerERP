package com.print3dmanager.erp.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Registro de falha de um job de impressão")
public record PrintFailRequest(

        @NotBlank(message = "O motivo da falha é obrigatório")
        String motivoFalha,

        @Schema(description = "Momento da falha; se omitido, assume agora")
        Instant finalizadoEm,

        @Schema(description = "Gramas desperdiçadas até a falha — abatidas do estoque do filamento",
                example = "12.3")
        @Positive(message = "O peso utilizado deve ser maior que zero")
        BigDecimal pesoUtilizadoG,

        @Schema(description = "Energia consumida até a falha, em kWh", example = "0.30")
        @PositiveOrZero(message = "O consumo de energia não pode ser negativo")
        BigDecimal consumoEnergiaKwh,

        String observacoes
) {
}
