package com.print3dmanager.erp.financial.dto;

import com.print3dmanager.erp.financial.model.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mudança de situação da transação (transições válidas: "
        + "PENDENTE→PAGA|CANCELADA, PAGA→PENDENTE (estorno da baixa); CANCELADA é terminal)")
public record FinancialTransactionStatusRequest(

        @NotNull(message = "O status é obrigatório")
        TransactionStatus status
) {
}
