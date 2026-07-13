package com.print3dmanager.erp.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para atualização de item de estoque "
        + "(quantidade muda apenas pelo endpoint de movimentação)")
public record InventoryItemUpdateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @Schema(description = "Unidade de medida (UN, KG, M, CX...)", example = "UN")
        @NotBlank(message = "A unidade de medida é obrigatória")
        @Size(max = 10, message = "A unidade de medida deve ter no máximo 10 caracteres")
        String unidadeMedida,

        @Schema(description = "Quantidade mínima para alerta de estoque baixo", example = "10")
        @NotNull(message = "A quantidade mínima é obrigatória")
        @PositiveOrZero(message = "A quantidade mínima não pode ser negativa")
        BigDecimal quantidadeMinima,

        @Positive(message = "O custo unitário deve ser maior que zero")
        BigDecimal custoUnitario,

        @Size(max = 120, message = "A localização deve ter no máximo 120 caracteres")
        String localizacao
) {
}
