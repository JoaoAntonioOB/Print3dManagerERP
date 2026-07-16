package com.print3dmanager.erp.financial.dto;

import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para lançamento de uma transação financeira (receita ou despesa)")
public record FinancialTransactionCreateRequest(

        @NotNull(message = "O tipo é obrigatório")
        TransactionType tipo,

        @NotBlank(message = "A categoria é obrigatória")
        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        @Schema(description = "Categoria livre para agrupamento", example = "Energia elétrica")
        String categoria,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        @Schema(description = "Sempre positivo — o sinal é dado pelo tipo", example = "150.00")
        BigDecimal valor,

        @NotNull(message = "A data da transação é obrigatória")
        @Schema(description = "Data de competência do lançamento", example = "2026-07-14")
        LocalDate dataTransacao,

        @Schema(description = "Situação inicial; se omitida, assume PENDENTE "
                + "(CANCELADA não é aceita no cadastro)", example = "PENDENTE")
        TransactionStatus status,

        @Size(max = 30, message = "A forma de pagamento deve ter no máximo 30 caracteres")
        @Schema(example = "PIX")
        String formaPagamento,

        @Schema(description = "Pedido vinculado (opcional)")
        Long pedidoId,

        @Schema(description = "Cliente vinculado (opcional); se omitido com pedido informado, "
                + "herda o cliente do pedido")
        Long clienteId,

        String observacoes
) {
}
