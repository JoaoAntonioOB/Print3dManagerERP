package com.print3dmanager.erp.report.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.financial.dto.FinancialSummaryResponse;
import com.print3dmanager.erp.financial.service.FinancialTransactionService;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.report.dto.PdfReportFile;
import com.print3dmanager.erp.report.repository.ReportQueryRepository;
import com.print3dmanager.erp.report.service.pdf.PdfReportBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Montagem dos relatórios PDF: resolve o período (default: mês corrente,
 * UTC), busca os dados agregados e descreve o conteúdo para o
 * {@link PdfReportBuilder}. Valores em moeda no formato pt-BR.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter FORMATO_ARQUIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    /** Teto do intervalo do relatório — evita full scan/geração em memória sem limite. */
    private static final long MAX_DIAS_PERIODO = 366;

    private final ReportQueryRepository reportQueryRepository;
    private final FinancialTransactionService financialTransactionService;

    /** Período resolvido: datas inclusivas + janela [inicio, fim) para TIMESTAMPTZ. */
    private record Periodo(LocalDate de, LocalDate ate, Instant inicio, Instant fimExclusivo) {
    }

    @Transactional(readOnly = true)
    public PdfReportFile pedidos(LocalDate de, LocalDate ate, OrderStatus status) {
        Periodo periodo = resolverPeriodo(de, ate);
        List<Object[]> linhas = reportQueryRepository.pedidosDoPeriodo(
                periodo.inicio(), periodo.fimExclusivo(),
                status != null ? status.name() : null);

        long cancelados = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        PdfReportBuilder builder = new PdfReportBuilder("Relatório de Pedidos"
                + (status != null ? " — " + legivel(status.name()) : ""))
                .comPeriodo(periodo.de(), periodo.ate())
                .comColunas(
                        List.of("Número", "Abertura", "Cliente", "Status",
                                "Entrega prevista", "Valor"),
                        new float[] {1.6f, 1.1f, 2.6f, 1.4f, 1.4f, 1.2f},
                        new boolean[] {false, false, false, false, false, true});

        for (Object[] linha : linhas) {
            String situacao = (String) linha[3];
            BigDecimal valor = new BigDecimal(linha[5].toString());
            if (OrderStatus.CANCELADO.name().equals(situacao)) {
                cancelados++;
            } else {
                valorTotal = valorTotal.add(valor);
            }
            builder.comLinha(List.of(
                    (String) linha[0],
                    (String) linha[1],
                    (String) linha[2],
                    legivel(situacao),
                    linha[4] != null ? (String) linha[4] : "—",
                    moeda(valor)));
        }

        builder.comTotal("Pedidos no período:", String.valueOf(linhas.size()));
        if (cancelados > 0) {
            builder.comTotal("Cancelados:", String.valueOf(cancelados));
        }
        builder.comTotal("Valor total (exceto cancelados):", moeda(valorTotal));
        return new PdfReportFile(nomeArquivo("pedidos", periodo), builder.gerar());
    }

    @Transactional(readOnly = true)
    public PdfReportFile financeiro(LocalDate de, LocalDate ate) {
        Periodo periodo = resolverPeriodo(de, ate);
        List<Object[]> linhas =
                reportQueryRepository.transacoesDoPeriodo(periodo.de(), periodo.ate());
        FinancialSummaryResponse resumo =
                financialTransactionService.resumo(periodo.de(), periodo.ate());

        PdfReportBuilder builder = new PdfReportBuilder("Relatório Financeiro")
                .comPeriodo(periodo.de(), periodo.ate())
                .comColunas(
                        List.of("Data", "Tipo", "Categoria", "Descrição", "Cliente",
                                "Status", "Valor"),
                        new float[] {1.1f, 1.0f, 1.6f, 2.6f, 1.8f, 1.2f, 1.2f},
                        new boolean[] {false, false, false, false, false, false, true});

        for (Object[] linha : linhas) {
            builder.comLinha(List.of(
                    (String) linha[0],
                    legivel((String) linha[1]),
                    (String) linha[2],
                    (String) linha[3],
                    linha[4] != null ? (String) linha[4] : "—",
                    legivel((String) linha[5]),
                    moeda(new BigDecimal(linha[6].toString()))));
        }

        builder.comTotal("Receitas pagas:", moeda(resumo.receitasPagas()))
                .comTotal("Receitas pendentes:", moeda(resumo.receitasPendentes()))
                .comTotal("Despesas pagas:", moeda(resumo.despesasPagas()))
                .comTotal("Despesas pendentes:", moeda(resumo.despesasPendentes()))
                .comTotal("Saldo realizado:", moeda(resumo.saldoRealizado()))
                .comTotal("Saldo previsto:", moeda(resumo.saldoPrevisto()));
        return new PdfReportFile(nomeArquivo("financeiro", periodo), builder.gerar());
    }

    @Transactional(readOnly = true)
    public PdfReportFile consumoFilamento(LocalDate de, LocalDate ate) {
        Periodo periodo = resolverPeriodo(de, ate);
        List<Object[]> linhas = reportQueryRepository.consumoPorFilamento(
                periodo.inicio(), periodo.fimExclusivo());

        long jobs = 0;
        BigDecimal gramas = BigDecimal.ZERO;
        BigDecimal custo = BigDecimal.ZERO;
        PdfReportBuilder builder = new PdfReportBuilder("Relatório de Consumo de Filamento")
                .comPeriodo(periodo.de(), periodo.ate())
                .comColunas(
                        List.of("Filamento", "Marca", "Material", "Cor", "Impressões",
                                "Consumo (g)", "Custo"),
                        new float[] {2.4f, 1.4f, 1.1f, 1.2f, 1.1f, 1.3f, 1.3f},
                        new boolean[] {false, false, false, false, true, true, true});

        for (Object[] linha : linhas) {
            long jobsFilamento = ((Number) linha[4]).longValue();
            BigDecimal gramasFilamento = new BigDecimal(linha[5].toString());
            BigDecimal custoFilamento =
                    linha[6] != null ? new BigDecimal(linha[6].toString()) : null;
            jobs += jobsFilamento;
            gramas = gramas.add(gramasFilamento);
            if (custoFilamento != null) {
                custo = custo.add(custoFilamento);
            }
            builder.comLinha(List.of(
                    (String) linha[0],
                    linha[1] != null ? (String) linha[1] : "—",
                    (String) linha[2],
                    linha[3] != null ? (String) linha[3] : "—",
                    String.valueOf(jobsFilamento),
                    numero(gramasFilamento),
                    custoFilamento != null ? moeda(custoFilamento) : "—"));
        }

        builder.comTotal("Impressões com consumo:", String.valueOf(jobs))
                .comTotal("Consumo total (g):", numero(gramas))
                .comTotal("Custo total registrado:", moeda(custo));
        return new PdfReportFile(nomeArquivo("consumo-filamento", periodo), builder.gerar());
    }

    /** Sem período informado, considera o mês corrente (UTC, o fuso do banco). */
    private Periodo resolverPeriodo(LocalDate de, LocalDate ate) {
        YearMonth mesAtual = YearMonth.now(ZoneOffset.UTC);
        LocalDate inicio = de != null ? de : mesAtual.atDay(1);
        LocalDate fim = ate != null ? ate : mesAtual.atEndOfMonth();
        if (inicio.isAfter(fim)) {
            throw new BusinessException(
                    "O início do período (de) não pode ser posterior ao fim (ate).");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAX_DIAS_PERIODO) {
            throw new BusinessException(
                    "O período do relatório não pode exceder %d dias."
                            .formatted(MAX_DIAS_PERIODO));
        }
        return new Periodo(inicio, fim,
                inicio.atStartOfDay(ZoneOffset.UTC).toInstant(),
                fim.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private String nomeArquivo(String relatorio, Periodo periodo) {
        return "relatorio-%s_%s-%s.pdf".formatted(relatorio,
                FORMATO_ARQUIVO.format(periodo.de()), FORMATO_ARQUIVO.format(periodo.ate()));
    }

    private String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    private String numero(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }

    /** ENUM_COM_UNDERSCORE → "Enum com underscore" legível no PDF. */
    private String legivel(String valorEnum) {
        String texto = valorEnum.replace('_', ' ').toLowerCase(PT_BR);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
