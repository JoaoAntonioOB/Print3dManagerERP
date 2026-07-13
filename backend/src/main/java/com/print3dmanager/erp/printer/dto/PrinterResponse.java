package com.print3dmanager.erp.printer.dto;

import com.print3dmanager.erp.printer.model.PrinterStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Dados de uma impressora 3D")
public record PrinterResponse(
        Long id,
        String nome,
        String marca,
        String modelo,
        PrinterStatus status,
        Integer potenciaWatts,
        Integer volumeXMm,
        Integer volumeYMm,
        Integer volumeZMm,
        BigDecimal horasImpressaoTotal,
        BigDecimal valorAquisicao,
        LocalDate dataAquisicao,
        String observacoes,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
