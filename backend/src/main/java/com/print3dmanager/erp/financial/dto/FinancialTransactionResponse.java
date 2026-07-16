package com.print3dmanager.erp.financial.dto;

import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Dados de uma transação financeira")
public record FinancialTransactionResponse(
        Long id,
        TransactionType tipo,
        String categoria,
        String descricao,
        @Schema(description = "Sempre positivo — o sinal é dado pelo tipo")
        BigDecimal valor,
        LocalDate dataTransacao,
        TransactionStatus status,
        String formaPagamento,
        Long pedidoId,
        String pedidoNumero,
        Long clienteId,
        String clienteNome,
        String observacoes,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
