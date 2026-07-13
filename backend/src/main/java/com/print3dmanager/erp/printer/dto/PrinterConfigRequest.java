package com.print3dmanager.erp.printer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Parâmetros de custo para o cálculo de orçamentos")
public record PrinterConfigRequest(

        @Schema(description = "Tarifa de energia em R$/kWh", example = "0.92")
        @NotNull(message = "O valor do kWh é obrigatório")
        @DecimalMin(value = "0", message = "O valor do kWh não pode ser negativo")
        BigDecimal valorKwh,

        @Schema(description = "Valor da hora-máquina em R$", example = "8.50")
        @NotNull(message = "O valor da hora-máquina é obrigatório")
        @DecimalMin(value = "0", message = "O valor da hora-máquina não pode ser negativo")
        BigDecimal valorHoraMaquina,

        @Schema(description = "Custo de desgaste por hora de impressão em R$", example = "1.20")
        @NotNull(message = "O custo de desgaste por hora é obrigatório")
        @DecimalMin(value = "0", message = "O custo de desgaste não pode ser negativo")
        BigDecimal custoDesgasteHora,

        @Schema(description = "Margem de lucro padrão em %", example = "100.00")
        @NotNull(message = "O markup padrão é obrigatório")
        @DecimalMin(value = "0", message = "O markup não pode ser negativo")
        BigDecimal markupPadrao
) {
}
