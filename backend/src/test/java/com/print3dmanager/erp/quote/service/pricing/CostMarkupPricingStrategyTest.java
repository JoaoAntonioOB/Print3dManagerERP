package com.print3dmanager.erp.quote.service.pricing;

import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da regra de negócio central do sistema:
 * custo filamento + energia + hora máquina + desgaste + margem de lucro.
 */
class CostMarkupPricingStrategyTest {

    private final CostMarkupPricingStrategy strategy = new CostMarkupPricingStrategy();

    @Test
    @DisplayName("cenário completo: soma dos 4 componentes com markup sobre o total")
    void calculaTodosOsComponentesComMarkup() {
        // 250 g × R$ 90/kg = 22,50 · 200 W × 5 h × R$ 0,95/kWh = 0,95
        // 5 h × R$ 8,00 = 40,00 · 5 h × R$ 1,50 = 7,50 → custo 70,95 · markup 100% → 141,90
        PricingInput input = new PricingInput(
                filamento("90.00"),
                impressora(200),
                Optional.of(configuracao("0.95", "8.00", "1.50")),
                new BigDecimal("250"),
                300,
                new BigDecimal("100"));

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.custoFilamento()).isEqualByComparingTo("22.50");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.95");
        assertThat(resultado.custoHoraMaquina()).isEqualByComparingTo("40.00");
        assertThat(resultado.custoDesgaste()).isEqualByComparingTo("7.50");
        assertThat(resultado.precoSugerido()).isEqualByComparingTo("141.90");
    }

    @Test
    @DisplayName("sem configuração de custos, só o filamento entra na conta")
    void semConfiguracaoCalculaApenasFilamento() {
        PricingInput input = new PricingInput(
                filamento("90.00"),
                impressora(200),
                Optional.empty(),
                new BigDecimal("250"),
                300,
                new BigDecimal("50"));

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.custoFilamento()).isEqualByComparingTo("22.50");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoHoraMaquina()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoDesgaste()).isEqualByComparingTo("0.00");
        assertThat(resultado.precoSugerido()).isEqualByComparingTo("33.75");
    }

    @Test
    @DisplayName("sem tempo de impressão, energia, hora máquina e desgaste zeram")
    void semTempoZeraComponentesPorHora() {
        PricingInput input = new PricingInput(
                filamento("90.00"),
                impressora(200),
                Optional.of(configuracao("0.95", "8.00", "1.50")),
                new BigDecimal("100"),
                null,
                new BigDecimal("100"));

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.custoFilamento()).isEqualByComparingTo("9.00");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoHoraMaquina()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoDesgaste()).isEqualByComparingTo("0.00");
        assertThat(resultado.precoSugerido()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("impressora sem potência cadastrada não gera custo de energia")
    void impressoraSemPotenciaNaoGeraEnergia() {
        PricingInput input = new PricingInput(
                null,
                impressora(null),
                Optional.of(configuracao("0.95", "8.00", "0.00")),
                null,
                60,
                new BigDecimal("100"));

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoHoraMaquina()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("sem nenhum dado, todos os componentes e o preço são zero")
    void semDadosRetornaZeros() {
        PricingInput input = new PricingInput(
                null, null, Optional.empty(), null, null, new BigDecimal("100"));

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.custoFilamento()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoEnergia()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoHoraMaquina()).isEqualByComparingTo("0.00");
        assertThat(resultado.custoDesgaste()).isEqualByComparingTo("0.00");
        assertThat(resultado.precoSugerido()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("componentes são arredondados antes da soma: a conta exibida fecha com o preço")
    void arredondaComponentesAntesDaSoma() {
        // 333 g × R$ 29,90/kg = 9,9567 → 9,96 · 1,5 h × R$ 7,77 = 11,655 → 11,66
        // custo 21,62 · markup 30% → 28,106 → 28,11
        PricingInput input = new PricingInput(
                filamento("29.90"),
                impressora(null),
                Optional.of(configuracao("0.95", "7.77", "0.00")),
                new BigDecimal("333"),
                90,
                new BigDecimal("30"));

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.custoFilamento()).isEqualByComparingTo("9.96");
        assertThat(resultado.custoHoraMaquina()).isEqualByComparingTo("11.66");
        BigDecimal somaExibida = resultado.custoFilamento()
                .add(resultado.custoEnergia())
                .add(resultado.custoHoraMaquina())
                .add(resultado.custoDesgaste());
        assertThat(somaExibida).isEqualByComparingTo("21.62");
        assertThat(resultado.precoSugerido()).isEqualByComparingTo("28.11");
    }

    @Test
    @DisplayName("markup zero devolve o preço igual ao custo total")
    void markupZeroPrecoIgualAoCusto() {
        PricingInput input = new PricingInput(
                filamento("100.00"),
                null,
                Optional.empty(),
                new BigDecimal("500"),
                null,
                BigDecimal.ZERO);

        PricingResult resultado = strategy.calcular(input);

        assertThat(resultado.precoSugerido()).isEqualByComparingTo("50.00");
    }

    private Filament filamento(String custoPorKg) {
        Filament filamento = new Filament();
        filamento.setNome("PLA Teste");
        filamento.setCustoPorKg(new BigDecimal(custoPorKg));
        return filamento;
    }

    private Printer impressora(Integer potenciaWatts) {
        Printer impressora = new Printer();
        impressora.setNome("Impressora Teste");
        impressora.setPotenciaWatts(potenciaWatts);
        return impressora;
    }

    private PrinterConfiguration configuracao(String valorKwh, String valorHoraMaquina,
                                              String custoDesgasteHora) {
        PrinterConfiguration config = new PrinterConfiguration();
        config.setValorKwh(new BigDecimal(valorKwh));
        config.setValorHoraMaquina(new BigDecimal(valorHoraMaquina));
        config.setCustoDesgasteHora(new BigDecimal(custoDesgasteHora));
        config.setMarkupPadrao(new BigDecimal("100"));
        return config;
    }
}
