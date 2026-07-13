package com.print3dmanager.erp.quote.dto;

import com.print3dmanager.erp.quote.model.QuoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mudança de status do orçamento (transições válidas: RASCUNHO→ENVIADO, "
        + "ENVIADO→RASCUNHO|APROVADO|REJEITADO|EXPIRADO; CONVERTIDO só via conversão)")
public record QuoteStatusRequest(

        @NotNull(message = "O status é obrigatório")
        QuoteStatus status
) {
}
