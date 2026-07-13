package com.print3dmanager.erp.printer.dto;

import com.print3dmanager.erp.printer.model.PrinterStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mudança da situação operacional da impressora")
public record PrinterStatusRequest(

        @NotNull(message = "O status é obrigatório")
        PrinterStatus status
) {
}
