package com.print3dmanager.erp.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para atualização de orçamento (permitida apenas em RASCUNHO). "
        + "Custos e preço sugerido são recalculados.")
public record QuoteUpdateRequest(

        @NotNull(message = "O cliente é obrigatório")
        Long clienteId,

        Long impressoraId,

        Long filamentoId,

        String descricao,

        LocalDate dataValidade,

        @Positive(message = "O tempo de impressão deve ser maior que zero")
        Integer tempoImpressaoMinutos,

        @Positive(message = "O peso estimado deve ser maior que zero")
        BigDecimal pesoEstimadoG,

        @Schema(description = "Margem de lucro em %", example = "100.00")
        @NotNull(message = "O markup é obrigatório")
        @PositiveOrZero(message = "O markup não pode ser negativo")
        @DecimalMax(value = "999.99", message = "O markup deve ser no máximo 999.99")
        BigDecimal markup,

        @Schema(description = "Preço final negociado; null volta a valer o preço sugerido")
        @Positive(message = "O preço final deve ser maior que zero")
        BigDecimal precoFinal,

        String observacoes
) {
}
