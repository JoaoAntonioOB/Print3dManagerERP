package com.print3dmanager.erp.quote.service.pricing;

/**
 * Estratégia de precificação de orçamentos (padrão Strategy).
 * A política padrão é {@link CostMarkupPricingStrategy}; novas políticas
 * (ex.: preço tabelado por grama) implementam esta interface e substituem
 * ou complementam a padrão sem alterar o QuoteService.
 */
public interface PricingStrategy {

    PricingResult calcular(PricingInput input);
}
