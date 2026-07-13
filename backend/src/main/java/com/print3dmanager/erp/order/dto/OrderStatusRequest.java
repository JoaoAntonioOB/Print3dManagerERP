package com.print3dmanager.erp.order.dto;

import com.print3dmanager.erp.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mudança de status do pedido (transições válidas: "
        + "PENDENTE→EM_PRODUCAO|CANCELADO, EM_PRODUCAO→CONCLUIDO|CANCELADO, CONCLUIDO→ENTREGUE)")
public record OrderStatusRequest(

        @NotNull(message = "O status é obrigatório")
        OrderStatus status
) {
}
