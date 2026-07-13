package com.print3dmanager.erp.printer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados para atualização de impressora (status tem endpoint próprio)")
public record PrinterUpdateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 80, message = "A marca deve ter no máximo 80 caracteres")
        String marca,

        @Size(max = 80, message = "O modelo deve ter no máximo 80 caracteres")
        String modelo,

        @Positive(message = "A potência deve ser maior que zero")
        Integer potenciaWatts,

        @Positive(message = "O volume X deve ser maior que zero")
        Integer volumeXMm,

        @Positive(message = "O volume Y deve ser maior que zero")
        Integer volumeYMm,

        @Positive(message = "O volume Z deve ser maior que zero")
        Integer volumeZMm,

        @PositiveOrZero(message = "O valor de aquisição não pode ser negativo")
        BigDecimal valorAquisicao,

        LocalDate dataAquisicao,

        String observacoes
) {
}
