package com.print3dmanager.erp.filament.dto;

import com.print3dmanager.erp.filament.model.FilamentMaterial;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastro de filamento/resina")
public record FilamentCreateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 80, message = "A marca deve ter no máximo 80 caracteres")
        String marca,

        @NotNull(message = "O material é obrigatório")
        FilamentMaterial material,

        @Size(max = 40, message = "A cor deve ter no máximo 40 caracteres")
        String cor,

        @Schema(description = "Diâmetro em milímetros; se omitido, assume 1.75", example = "1.75")
        @Positive(message = "O diâmetro deve ser maior que zero")
        BigDecimal diametroMm,

        @Schema(description = "Peso da bobina cheia em gramas", example = "1000")
        @Positive(message = "O peso da bobina deve ser maior que zero")
        BigDecimal pesoBobinaG,

        @Schema(description = "Custo por quilograma, base do custo de material dos orçamentos",
                example = "120.00")
        @NotNull(message = "O custo por kg é obrigatório")
        @Positive(message = "O custo por kg deve ser maior que zero")
        BigDecimal custoPorKg,

        @Schema(description = "Estoque inicial em gramas; se omitido, assume 0", example = "1000")
        @PositiveOrZero(message = "O estoque inicial não pode ser negativo")
        BigDecimal quantidadeEstoqueG,

        @Schema(description = "Estoque mínimo em gramas para alerta; se omitido, assume 0",
                example = "200")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
        BigDecimal estoqueMinimoG,

        @Schema(description = "Temperatura recomendada do bico em °C", example = "210")
        @Positive(message = "A temperatura do bico deve ser maior que zero")
        Integer temperaturaBico,

        @Schema(description = "Temperatura recomendada da mesa em °C", example = "60")
        @PositiveOrZero(message = "A temperatura da mesa não pode ser negativa")
        Integer temperaturaMesa
) {
}
