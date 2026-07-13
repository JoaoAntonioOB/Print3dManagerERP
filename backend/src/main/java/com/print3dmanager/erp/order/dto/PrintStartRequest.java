package com.print3dmanager.erp.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Início de um job de impressão (a impressora fica IMPRIMINDO)")
public record PrintStartRequest(

        @NotNull(message = "A impressora é obrigatória")
        Long impressoraId,

        @Schema(description = "Filamento usado no job (necessário para consumo de estoque)")
        Long filamentoId,

        @Schema(description = "Item de pedido sendo produzido; null = impressão avulsa")
        Long itemPedidoId,

        @Schema(description = "Momento de início; se omitido, assume agora")
        Instant iniciadoEm,

        String observacoes
) {
}
