package com.print3dmanager.erp.quote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para criação de orçamento (status inicial: RASCUNHO). "
        + "Custos e preço sugerido são calculados pela aplicação.")
public record QuoteCreateRequest(

        @NotNull(message = "O cliente é obrigatório")
        Long clienteId,

        @Schema(description = "Impressora prevista — habilita custo de energia (potência) e "
                + "a configuração de custos própria dela")
        Long impressoraId,

        @Schema(description = "Filamento previsto — habilita o custo de material")
        Long filamentoId,

        @Schema(description = "Descrição da peça/serviço orçado", example = "Suporte de parede")
        String descricao,

        @Schema(description = "Data limite para o cliente aprovar; vencida, o link expira")
        LocalDate dataValidade,

        @Schema(description = "Tempo estimado de impressão, em minutos", example = "180")
        @Positive(message = "O tempo de impressão deve ser maior que zero")
        Integer tempoImpressaoMinutos,

        @Schema(description = "Peso estimado da peça, em gramas", example = "150")
        @Positive(message = "O peso estimado deve ser maior que zero")
        BigDecimal pesoEstimadoG,

        @Schema(description = "Margem de lucro em %; se omitida, usa o markup padrão da "
                + "configuração efetiva da impressora (fallback: 100)", example = "100.00")
        @PositiveOrZero(message = "O markup não pode ser negativo")
        @DecimalMax(value = "999.99", message = "O markup deve ser no máximo 999.99")
        BigDecimal markup,

        @Schema(description = "Preço final negociado; se omitido, vale o preço sugerido")
        @Positive(message = "O preço final deve ser maior que zero")
        BigDecimal precoFinal,

        String observacoes
) {
}
