package com.print3dmanager.erp.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Item (peça) de um pedido")
public record OrderItemRequest(

        @Schema(description = "Id do item existente na atualização; null = item novo. "
                + "Itens do pedido ausentes da lista são removidos.")
        Long id,

        @Schema(description = "Filamento previsto para a peça")
        Long filamentoId,

        @NotBlank(message = "O nome da peça é obrigatório")
        @Size(max = 160, message = "O nome da peça deve ter no máximo 160 caracteres")
        String nomePeca,

        String descricao,

        @Schema(description = "Quantidade de unidades da peça; se omitida, assume 1", example = "1")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @Schema(description = "Peso estimado por unidade, em gramas", example = "35.5")
        @Positive(message = "O peso estimado deve ser maior que zero")
        BigDecimal pesoEstimadoG,

        @Schema(description = "Tempo estimado de impressão por unidade, em minutos", example = "90")
        @Positive(message = "O tempo de impressão deve ser maior que zero")
        Integer tempoImpressaoMinutos,

        @NotNull(message = "O preço unitário é obrigatório")
        @PositiveOrZero(message = "O preço unitário não pode ser negativo")
        BigDecimal precoUnitario
) {
}
