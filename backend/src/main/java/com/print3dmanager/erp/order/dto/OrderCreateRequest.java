package com.print3dmanager.erp.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Dados para abertura de pedido (status inicial: PENDENTE; "
        + "número gerado pela aplicação)")
public record OrderCreateRequest(

        @NotNull(message = "O cliente é obrigatório")
        Long clienteId,

        LocalDate dataEntregaPrevista,

        @Schema(description = "Desconto em valor absoluto sobre o total; se omitido, assume 0",
                example = "10.00")
        @PositiveOrZero(message = "O desconto não pode ser negativo")
        BigDecimal desconto,

        String observacoes,

        @NotEmpty(message = "O pedido deve ter pelo menos um item")
        List<@Valid OrderItemRequest> itens
) {
}
