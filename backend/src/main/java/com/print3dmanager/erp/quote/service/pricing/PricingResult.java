package com.print3dmanager.erp.quote.service.pricing;

import java.math.BigDecimal;

/**
 * Custos decompostos e preço sugerido de um orçamento, prontos para
 * gravação nas colunas próprias de {@code orcamentos}.
 */
public record PricingResult(
        BigDecimal custoFilamento,
        BigDecimal custoEnergia,
        BigDecimal custoHoraMaquina,
        BigDecimal custoDesgaste,
        BigDecimal precoSugerido
) {
}
