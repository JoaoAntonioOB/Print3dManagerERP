package com.print3dmanager.erp.filament.dto;

import com.print3dmanager.erp.filament.model.FilamentMaterial;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Dados de um filamento/resina")
public record FilamentResponse(
        Long id,
        String nome,
        String marca,
        FilamentMaterial material,
        String cor,
        BigDecimal diametroMm,
        BigDecimal pesoBobinaG,
        BigDecimal custoPorKg,
        BigDecimal quantidadeEstoqueG,
        BigDecimal estoqueMinimoG,
        @Schema(description = "true quando a quantidade em estoque é menor ou igual ao mínimo")
        boolean estoqueBaixo,
        Integer temperaturaBico,
        Integer temperaturaMesa,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
