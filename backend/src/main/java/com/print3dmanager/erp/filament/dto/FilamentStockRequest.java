package com.print3dmanager.erp.filament.dto;

import com.print3dmanager.erp.filament.model.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Movimentação manual de estoque em gramas (ENTRADA repõe, SAIDA consome)")
public record FilamentStockRequest(

        @NotNull(message = "O tipo de movimentação é obrigatório")
        StockMovementType tipo,

        @Schema(description = "Quantidade movimentada em gramas", example = "250")
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        BigDecimal quantidadeG
) {
}
