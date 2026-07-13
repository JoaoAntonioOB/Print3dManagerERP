package com.print3dmanager.erp.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastro de item de estoque (insumos gerais, exceto filamentos)")
public record InventoryItemCreateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @Schema(description = "Quantidade inicial em estoque; se omitida, assume 0", example = "50")
        @PositiveOrZero(message = "A quantidade inicial não pode ser negativa")
        BigDecimal quantidade,

        @Schema(description = "Unidade de medida (UN, KG, M, CX...); se omitida, assume UN",
                example = "UN")
        @Size(max = 10, message = "A unidade de medida deve ter no máximo 10 caracteres")
        String unidadeMedida,

        @Schema(description = "Quantidade mínima para alerta de estoque baixo; se omitida, assume 0",
                example = "10")
        @PositiveOrZero(message = "A quantidade mínima não pode ser negativa")
        BigDecimal quantidadeMinima,

        @Schema(description = "Custo unitário do insumo", example = "4.90")
        @Positive(message = "O custo unitário deve ser maior que zero")
        BigDecimal custoUnitario,

        @Schema(description = "Onde o item está guardado", example = "Prateleira B3")
        @Size(max = 120, message = "A localização deve ter no máximo 120 caracteres")
        String localizacao
) {
}
