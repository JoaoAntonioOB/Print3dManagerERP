package com.print3dmanager.erp.dashboard.service;

import com.print3dmanager.erp.dashboard.dto.DashboardResumoResponse;
import com.print3dmanager.erp.dashboard.dto.MonthlyFilamentUsagePoint;
import com.print3dmanager.erp.dashboard.dto.MonthlySalesPoint;
import com.print3dmanager.erp.dashboard.dto.PrintSuccessRateResponse;
import com.print3dmanager.erp.dashboard.dto.TopClientPoint;
import com.print3dmanager.erp.dashboard.repository.DashboardQueryRepository;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.model.PrintStatus;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import com.print3dmanager.erp.quote.model.QuoteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Indicadores agregados do painel. Séries mensais sempre devolvem os N
 * meses completos (faltantes preenchidos com zero) em ordem cronológica,
 * prontos para os gráficos do frontend.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MESES_MAX = 60;
    private static final int LIMITE_CLIENTES_MAX = 50;

    private final DashboardQueryRepository dashboardQueryRepository;

    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo() {
        YearMonth mesAtual = YearMonth.now(ZoneOffset.UTC);
        Instant inicioMes = mesAtual.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return new DashboardResumoResponse(
                contagemPorStatus("pedidos", OrderStatus.values()),
                contagemPorStatus("orcamentos", QuoteStatus.values()),
                contagemPorStatus("impressoras", PrinterStatus.values()),
                dashboardQueryRepository.contarFilamentosEstoqueBaixo(),
                dashboardQueryRepository.contarItensEstoqueBaixo(),
                dashboardQueryRepository.contarClientesAtivos(),
                dashboardQueryRepository.contarImpressoesEmAndamento(),
                dashboardQueryRepository.contarPedidosDesde(inicioMes),
                dashboardQueryRepository.somarFaturamentoEntregueDesde(mesAtual.atDay(1)));
    }

    @Transactional(readOnly = true)
    public List<MonthlySalesPoint> vendasMensais(int meses) {
        List<YearMonth> janela = janelaDeMeses(meses);
        Instant inicioInstant = inicioDaJanela(janela);
        LocalDate inicioData = janela.get(0).atDay(1);

        Map<String, Long> pedidos = mapear(
                dashboardQueryRepository.pedidosPorMes(inicioInstant),
                valor -> ((Number) valor).longValue());
        Map<String, BigDecimal> faturamento = mapear(
                dashboardQueryRepository.faturamentoPorMes(inicioData),
                valor -> new BigDecimal(valor.toString()));

        return janela.stream()
                .map(FORMATO_MES::format)
                .map(mes -> new MonthlySalesPoint(
                        mes,
                        pedidos.getOrDefault(mes, 0L),
                        faturamento.getOrDefault(mes, BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyFilamentUsagePoint> consumoFilamentoMensal(int meses) {
        List<YearMonth> janela = janelaDeMeses(meses);
        Map<String, BigDecimal> consumo = mapear(
                dashboardQueryRepository.consumoFilamentoPorMes(inicioDaJanela(janela)),
                valor -> new BigDecimal(valor.toString()));

        return janela.stream()
                .map(FORMATO_MES::format)
                .map(mes -> new MonthlyFilamentUsagePoint(
                        mes, consumo.getOrDefault(mes, BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PrintSuccessRateResponse taxaSucessoImpressoes() {
        Map<String, Long> porStatus = contagemPorStatus("historico_impressoes",
                PrintStatus.values());
        long concluidas = porStatus.get(PrintStatus.CONCLUIDA.name());
        long falhas = porStatus.get(PrintStatus.FALHOU.name());
        long finalizadas = concluidas + falhas;

        BigDecimal taxa = finalizadas == 0 ? null
                : BigDecimal.valueOf(concluidas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(finalizadas), 2, RoundingMode.HALF_UP);
        return new PrintSuccessRateResponse(
                concluidas,
                falhas,
                porStatus.get(PrintStatus.CANCELADA.name()),
                porStatus.get(PrintStatus.EM_ANDAMENTO.name()),
                taxa);
    }

    @Transactional(readOnly = true)
    public List<TopClientPoint> topClientes(int limite) {
        int limiteEfetivo = Math.clamp(limite, 1, LIMITE_CLIENTES_MAX);
        return dashboardQueryRepository.topClientes(limiteEfetivo).stream()
                .map(linha -> new TopClientPoint(
                        ((Number) linha[0]).longValue(),
                        (String) linha[1],
                        ((Number) linha[2]).longValue(),
                        new BigDecimal(linha[3].toString())))
                .toList();
    }

    /** Contagem por status com todos os valores do enum presentes (ausência = 0). */
    private <E extends Enum<E>> Map<String, Long> contagemPorStatus(String tabela, E[] valores) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (E valor : valores) {
            contagem.put(valor.name(), 0L);
        }
        dashboardQueryRepository.contarPorStatus(tabela)
                .forEach(linha -> contagem.put((String) linha[0],
                        ((Number) linha[1]).longValue()));
        return contagem;
    }

    /** Últimos N meses (1–60), incluindo o corrente, em ordem cronológica. */
    private List<YearMonth> janelaDeMeses(int meses) {
        int quantidade = Math.clamp(meses, 1, MESES_MAX);
        YearMonth atual = YearMonth.now(ZoneOffset.UTC);
        List<YearMonth> janela = new ArrayList<>(quantidade);
        for (int i = quantidade - 1; i >= 0; i--) {
            janela.add(atual.minusMonths(i));
        }
        return janela;
    }

    private Instant inicioDaJanela(List<YearMonth> janela) {
        return janela.get(0).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private <V> Map<String, V> mapear(List<Object[]> linhas, Function<Object, V> conversor) {
        Map<String, V> mapa = new LinkedHashMap<>();
        linhas.forEach(linha -> mapa.put((String) linha[0], conversor.apply(linha[1])));
        return mapa;
    }
}
