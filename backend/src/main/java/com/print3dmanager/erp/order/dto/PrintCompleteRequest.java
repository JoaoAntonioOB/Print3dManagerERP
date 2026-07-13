package com.print3dmanager.erp.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Finalização de um job de impressão (conclusão ou cancelamento)")
public record PrintCompleteRequest(

        @Schema(description = "Momento de término; se omitido, assume agora")
        Instant finalizadoEm,

        @Schema(description = "Gramas efetivamente consumidas — abatidas do estoque do filamento",
                example = "42.7")
        @Positive(message = "O peso utilizado deve ser maior que zero")
        BigDecimal pesoUtilizadoG,

        @Schema(description = "Energia consumida no job, em kWh", example = "0.85")
        @PositiveOrZero(message = "O consumo de energia não pode ser negativo")
        BigDecimal consumoEnergiaKwh,

        String observacoes
) {
}
