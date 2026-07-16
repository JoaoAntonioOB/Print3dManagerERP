package com.print3dmanager.erp.financial.dto;

import com.print3dmanager.erp.financial.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para atualização de uma transação PENDENTE — "
        + "a situação muda apenas pelo endpoint de status")
public record FinancialTransactionUpdateRequest(

        @NotNull(message = "O tipo é obrigatório")
        TransactionType tipo,

        @NotBlank(message = "A categoria é obrigatória")
        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "A data da transação é obrigatória")
        LocalDate dataTransacao,

        @Size(max = 30, message = "A forma de pagamento deve ter no máximo 30 caracteres")
        String formaPagamento,

        @Schema(description = "Pedido vinculado; null remove o vínculo")
        Long pedidoId,

        @Schema(description = "Cliente vinculado; se omitido com pedido informado, "
                + "herda o cliente do pedido")
        Long clienteId,

        String observacoes
) {
}
