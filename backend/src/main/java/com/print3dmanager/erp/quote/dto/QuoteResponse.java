package com.print3dmanager.erp.quote.dto;

import com.print3dmanager.erp.quote.model.QuoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Orçamento completo (visão interna, com custos e markup)")
public record QuoteResponse(
        Long id,
        String numero,
        Long clienteId,
        String clienteNome,
        @Schema(description = "Usuário que elaborou o orçamento")
        Long usuarioId,
        String usuarioNome,
        Long impressoraId,
        String impressoraNome,
        Long filamentoId,
        String filamentoNome,
        @Schema(description = "Pedido gerado na conversão, quando houver")
        Long pedidoId,
        String pedidoNumero,
        QuoteStatus status,
        @Schema(description = "Token do link público de aprovação "
                + "(GET /public/quotes/{shareToken})")
        UUID shareToken,
        String descricao,
        LocalDate dataValidade,
        Integer tempoImpressaoMinutos,
        BigDecimal pesoEstimadoG,
        BigDecimal custoFilamento,
        BigDecimal custoEnergia,
        BigDecimal custoHoraMaquina,
        BigDecimal custoDesgaste,
        @Schema(description = "Soma dos custos decompostos")
        BigDecimal custoTotal,
        BigDecimal markup,
        BigDecimal precoSugerido,
        BigDecimal precoFinal,
        @Schema(description = "Preço apresentado ao cliente: final, se definido; senão o sugerido")
        BigDecimal precoEfetivo,
        Instant aprovadoEm,
        String observacoes,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
