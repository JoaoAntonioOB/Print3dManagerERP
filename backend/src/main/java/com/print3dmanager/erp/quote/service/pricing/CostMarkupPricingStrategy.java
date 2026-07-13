package com.print3dmanager.erp.quote.service.pricing;

import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Política padrão — a regra central do sistema:
 * custo filamento + energia + hora máquina + desgaste, com margem de
 * lucro percentual (markup) sobre o custo total.
 *
 * <p>Componentes sem dados suficientes entram como zero:
 * filamento exige filamento + peso; energia exige impressora com potência,
 * tempo e tarifa configurada; hora máquina e desgaste exigem tempo e
 * configuração de custos.</p>
 */
@Component
public class CostMarkupPricingStrategy implements PricingStrategy {

    private static final BigDecimal GRAMAS_POR_KG = new BigDecimal("1000");
    private static final BigDecimal WATTS_POR_KW = new BigDecimal("1000");
    private static final BigDecimal MINUTOS_POR_HORA = new BigDecimal("60");
    private static final BigDecimal CEM = new BigDecimal("100");

    @Override
    public PricingResult calcular(PricingInput input) {
        BigDecimal horas = input.tempoImpressaoMinutos() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(input.tempoImpressaoMinutos())
                        .divide(MINUTOS_POR_HORA, 4, RoundingMode.HALF_UP);

        // Componentes arredondados antes da soma: a conta exibida ao usuário
        // (custos somados × markup) fecha exatamente com o preço sugerido.
        BigDecimal custoFilamento = arredondar(calcularFilamento(input));
        BigDecimal custoEnergia = arredondar(calcularEnergia(input, horas));
        BigDecimal custoHoraMaquina = arredondar(calcularHoraMaquina(input, horas));
        BigDecimal custoDesgaste = arredondar(calcularDesgaste(input, horas));

        BigDecimal custoTotal = custoFilamento
                .add(custoEnergia)
                .add(custoHoraMaquina)
                .add(custoDesgaste);
        BigDecimal precoSugerido = custoTotal
                .multiply(BigDecimal.ONE.add(input.markup()
                        .divide(CEM, 6, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        return new PricingResult(custoFilamento, custoEnergia, custoHoraMaquina,
                custoDesgaste, precoSugerido);
    }

    /** peso (g) ÷ 1000 × custo por kg. */
    private BigDecimal calcularFilamento(PricingInput input) {
        if (input.filamento() == null || input.pesoEstimadoG() == null) {
            return BigDecimal.ZERO;
        }
        return input.pesoEstimadoG()
                .multiply(input.filamento().getCustoPorKg())
                .divide(GRAMAS_POR_KG, 4, RoundingMode.HALF_UP);
    }

    /** potência (W) ÷ 1000 × horas × tarifa (R$/kWh). */
    private BigDecimal calcularEnergia(PricingInput input, BigDecimal horas) {
        if (input.impressora() == null || input.impressora().getPotenciaWatts() == null
                || horas.signum() == 0 || input.configuracao().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(input.impressora().getPotenciaWatts())
                .divide(WATTS_POR_KW, 4, RoundingMode.HALF_UP)
                .multiply(horas)
                .multiply(input.configuracao().get().getValorKwh());
    }

    /** valor hora-máquina × horas. */
    private BigDecimal calcularHoraMaquina(PricingInput input, BigDecimal horas) {
        return input.configuracao()
                .filter(config -> horas.signum() > 0)
                .map(PrinterConfiguration::getValorHoraMaquina)
                .map(horas::multiply)
                .orElse(BigDecimal.ZERO);
    }

    /** custo de desgaste por hora × horas. */
    private BigDecimal calcularDesgaste(PricingInput input, BigDecimal horas) {
        return input.configuracao()
                .filter(config -> horas.signum() > 0)
                .map(PrinterConfiguration::getCustoDesgasteHora)
                .map(horas::multiply)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
