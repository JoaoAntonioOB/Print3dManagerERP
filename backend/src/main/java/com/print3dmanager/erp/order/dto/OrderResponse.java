package com.print3dmanager.erp.order.dto;

import com.print3dmanager.erp.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Pedido completo, com itens")
public record OrderResponse(
        Long id,
        String numero,
        Long clienteId,
        String clienteNome,
        @Schema(description = "Usuário que registrou o pedido")
        Long usuarioId,
        String usuarioNome,
        OrderStatus status,
        LocalDate dataEntregaPrevista,
        LocalDate dataEntregaRealizada,
        @Schema(description = "Soma dos subtotais dos itens menos o desconto")
        BigDecimal valorTotal,
        BigDecimal desconto,
        String observacoes,
        List<OrderItemResponse> itens,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
