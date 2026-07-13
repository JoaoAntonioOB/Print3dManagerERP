package com.print3dmanager.erp.quote.service.pricing;

import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Dados disponíveis para precificar um orçamento. Qualquer campo pode
 * faltar — a estratégia calcula apenas os componentes que têm dados.
 */
public record PricingInput(
        Filament filamento,
        Printer impressora,
        Optional<PrinterConfiguration> configuracao,
        BigDecimal pesoEstimadoG,
        Integer tempoImpressaoMinutos,
        BigDecimal markup
) {
}
