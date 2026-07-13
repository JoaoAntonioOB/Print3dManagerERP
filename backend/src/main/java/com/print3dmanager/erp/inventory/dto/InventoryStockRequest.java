package com.print3dmanager.erp.inventory.dto;

import com.print3dmanager.erp.common.model.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Movimentação manual de estoque na unidade do item "
        + "(ENTRADA repõe, SAIDA consome)")
public record InventoryStockRequest(

        @NotNull(message = "O tipo de movimentação é obrigatório")
        StockMovementType tipo,

        @Schema(description = "Quantidade movimentada, na unidade de medida do item", example = "5")
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        BigDecimal quantidade
) {
}
