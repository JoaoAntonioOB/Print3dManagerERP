package com.print3dmanager.erp.quote.dto;

import com.print3dmanager.erp.quote.model.QuoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Visão pública do orçamento para o cliente aprovar — sem custos "
        + "internos nem markup")
public record PublicQuoteResponse(
        String numero,
        String clienteNome,
        String descricao,
        QuoteStatus status,
        LocalDate dataValidade,
        Integer tempoImpressaoMinutos,
        BigDecimal pesoEstimadoG,
        @Schema(description = "Preço proposto ao cliente")
        BigDecimal preco,
        Instant aprovadoEm,
        Instant criadoEm
) {
}
