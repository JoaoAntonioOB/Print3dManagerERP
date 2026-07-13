package com.print3dmanager.erp.order.dto;

import com.print3dmanager.erp.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Resumo de pedido para listagens (sem itens)")
public record OrderSummaryResponse(
        Long id,
        String numero,
        Long clienteId,
        String clienteNome,
        OrderStatus status,
        LocalDate dataEntregaPrevista,
        LocalDate dataEntregaRealizada,
        BigDecimal valorTotal,
        BigDecimal desconto,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
