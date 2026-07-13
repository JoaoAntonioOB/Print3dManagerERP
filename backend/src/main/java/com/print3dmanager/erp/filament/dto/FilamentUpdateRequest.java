package com.print3dmanager.erp.filament.dto;

import com.print3dmanager.erp.filament.model.FilamentMaterial;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para atualização de filamento "
        + "(estoque muda apenas pelo endpoint de movimentação)")
public record FilamentUpdateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 80, message = "A marca deve ter no máximo 80 caracteres")
        String marca,

        @NotNull(message = "O material é obrigatório")
        FilamentMaterial material,

        @Size(max = 40, message = "A cor deve ter no máximo 40 caracteres")
        String cor,

        @Schema(description = "Diâmetro em milímetros", example = "1.75")
        @NotNull(message = "O diâmetro é obrigatório")
        @Positive(message = "O diâmetro deve ser maior que zero")
        BigDecimal diametroMm,

        @Positive(message = "O peso da bobina deve ser maior que zero")
        BigDecimal pesoBobinaG,

        @NotNull(message = "O custo por kg é obrigatório")
        @Positive(message = "O custo por kg deve ser maior que zero")
        BigDecimal custoPorKg,

        @Schema(description = "Estoque mínimo em gramas para alerta", example = "200")
        @NotNull(message = "O estoque mínimo é obrigatório")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
        BigDecimal estoqueMinimoG,

        @Positive(message = "A temperatura do bico deve ser maior que zero")
        Integer temperaturaBico,

        @PositiveOrZero(message = "A temperatura da mesa não pode ser negativa")
        Integer temperaturaMesa
) {
}
