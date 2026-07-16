package com.print3dmanager.erp.financial.service;

import com.print3dmanager.erp.client.repository.ClientRepository;
import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.financial.dto.FinancialSummaryResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionCreateRequest;
import com.print3dmanager.erp.financial.dto.FinancialTransactionResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionUpdateRequest;
import com.print3dmanager.erp.financial.dto.MonthlyFinancialPoint;
import com.print3dmanager.erp.financial.mapper.FinancialTransactionMapper;
import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import com.print3dmanager.erp.financial.repository.FinancialQueryRepository;
import com.print3dmanager.erp.financial.repository.FinancialTransactionRepository;
import com.print3dmanager.erp.financial.repository.FinancialTransactionSpecifications;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras de negócio de transações financeiras: lançamentos manuais com
 * vínculo opcional a pedido/cliente, ciclo de vida por máquina de estados
 * (PENDENTE→PAGA|CANCELADA, PAGA→PENDENTE) e resumos agregados por período.
 */
@Service
@RequiredArgsConstructor
public class FinancialTransactionService {

    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MESES_MAX = 60;

    /** Transições de status permitidas a partir de cada estado. */
    private static final Map<TransactionStatus, Set<TransactionStatus>> TRANSICOES = Map.of(
            TransactionStatus.PENDENTE, Set.of(TransactionStatus.PAGA,
                    TransactionStatus.CANCELADA),
            TransactionStatus.PAGA, Set.of(TransactionStatus.PENDENTE),
            TransactionStatus.CANCELADA, Set.of());

    private final FinancialTransactionRepository financialTransactionRepository;
    private final FinancialQueryRepository financialQueryRepository;
    private final FinancialTransactionMapper financialTransactionMapper;
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PageResponse<FinancialTransactionResponse> listar(String busca, TransactionType tipo,
                                                             TransactionStatus status,
                                                             String categoria, Long pedidoId,
                                                             Long clienteId, LocalDate de,
                                                             LocalDate ate, Pageable pageable) {
        return PageResponse.de(
                financialTransactionRepository.findAll(
                                FinancialTransactionSpecifications.comFiltros(busca, tipo, status,
                                        categoria, pedidoId, clienteId, de, ate), pageable)
                        .map(financialTransactionMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public FinancialTransactionResponse buscarPorId(Long id) {
        return financialTransactionMapper.toResponse(obterTransacaoDetalhada(id));
    }

    @Transactional
    public FinancialTransactionResponse criar(FinancialTransactionCreateRequest request) {
        if (request.status() == TransactionStatus.CANCELADA) {
            throw new BusinessException(
                    "Uma transação não pode ser cadastrada já CANCELADA.");
        }
        FinancialTransaction transacao = financialTransactionMapper.toEntity(request);
        if (request.status() != null) {
            transacao.setStatus(request.status());
        }
        vincularReferencias(transacao, request.pedidoId(), request.clienteId());
        return financialTransactionMapper.toResponse(
                financialTransactionRepository.save(transacao));
    }

    @Transactional
    public FinancialTransactionResponse atualizar(Long id,
                                                  FinancialTransactionUpdateRequest request) {
        FinancialTransaction transacao = obterTransacaoDetalhada(id);
        if (transacao.getStatus() != TransactionStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente transações PENDENTES podem ser editadas. Status atual: %s."
                            .formatted(transacao.getStatus()));
        }
        financialTransactionMapper.atualizar(transacao, request);
        vincularReferencias(transacao, request.pedidoId(), request.clienteId());
        return financialTransactionMapper.toResponse(transacao);
    }

    @Transactional
    public FinancialTransactionResponse alterarStatus(Long id, TransactionStatus novoStatus) {
        FinancialTransaction transacao = obterTransacaoDetalhada(id);
        if (!TRANSICOES.get(transacao.getStatus()).contains(novoStatus)) {
            throw new BusinessException(
                    "Transição de status inválida: %s → %s."
                            .formatted(transacao.getStatus(), novoStatus));
        }
        transacao.setStatus(novoStatus);
        return financialTransactionMapper.toResponse(transacao);
    }

    /** Exclusão física, permitida apenas para transações ainda PENDENTES. */
    @Transactional
    public void excluir(Long id) {
        FinancialTransaction transacao = obterTransacao(id);
        if (transacao.getStatus() != TransactionStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente transações PENDENTES podem ser excluídas — cancele a transação "
                            + "para preservar o histórico.");
        }
        financialTransactionRepository.delete(transacao);
    }

    /**
     * Totais do período por tipo e situação (CANCELADAS fora). Sem período
     * informado, considera o mês corrente (UTC, o fuso do banco).
     */
    @Transactional(readOnly = true)
    public FinancialSummaryResponse resumo(LocalDate de, LocalDate ate) {
        YearMonth mesAtual = YearMonth.now(ZoneOffset.UTC);
        LocalDate inicio = de != null ? de : mesAtual.atDay(1);
        LocalDate fim = ate != null ? ate : mesAtual.atEndOfMonth();
        if (inicio.isAfter(fim)) {
            throw new BusinessException(
                    "O início do período (de) não pode ser posterior ao fim (ate).");
        }

        Map<TransactionType, Map<TransactionStatus, BigDecimal>> totais =
                new EnumMap<>(TransactionType.class);
        financialQueryRepository.totaisPorTipoEStatus(inicio, fim).forEach(linha ->
                totais.computeIfAbsent(TransactionType.valueOf((String) linha[0]),
                                tipo -> new EnumMap<>(TransactionStatus.class))
                        .put(TransactionStatus.valueOf((String) linha[1]),
                                new BigDecimal(linha[2].toString())));

        BigDecimal receitasPagas = total(totais, TransactionType.RECEITA,
                TransactionStatus.PAGA);
        BigDecimal receitasPendentes = total(totais, TransactionType.RECEITA,
                TransactionStatus.PENDENTE);
        BigDecimal despesasPagas = total(totais, TransactionType.DESPESA,
                TransactionStatus.PAGA);
        BigDecimal despesasPendentes = total(totais, TransactionType.DESPESA,
                TransactionStatus.PENDENTE);

        return new FinancialSummaryResponse(
                inicio,
                fim,
                receitasPagas,
                receitasPendentes,
                despesasPagas,
                despesasPendentes,
                receitasPagas.subtract(despesasPagas),
                receitasPagas.add(receitasPendentes)
                        .subtract(despesasPagas).subtract(despesasPendentes));
    }

    /**
     * Série mensal do movimento realizado (transações PAGAS): sempre os N
     * meses completos (1–60, incluindo o corrente), faltantes zerados,
     * em ordem cronológica — pronta para os gráficos do frontend.
     */
    @Transactional(readOnly = true)
    public List<MonthlyFinancialPoint> resumoMensal(int meses) {
        int quantidade = Math.clamp(meses, 1, MESES_MAX);
        YearMonth atual = YearMonth.now(ZoneOffset.UTC);
        List<YearMonth> janela = new ArrayList<>(quantidade);
        for (int i = quantidade - 1; i >= 0; i--) {
            janela.add(atual.minusMonths(i));
        }

        Map<String, BigDecimal> receitas = new HashMap<>();
        Map<String, BigDecimal> despesas = new HashMap<>();
        financialQueryRepository.movimentoPagoPorMes(janela.get(0).atDay(1)).forEach(linha -> {
            Map<String, BigDecimal> destino =
                    TransactionType.valueOf((String) linha[1]) == TransactionType.RECEITA
                            ? receitas : despesas;
            destino.put((String) linha[0], new BigDecimal(linha[2].toString()));
        });

        return janela.stream()
                .map(FORMATO_MES::format)
                .map(mes -> {
                    BigDecimal receita = receitas.getOrDefault(mes, BigDecimal.ZERO);
                    BigDecimal despesa = despesas.getOrDefault(mes, BigDecimal.ZERO);
                    return new MonthlyFinancialPoint(mes, receita, despesa,
                            receita.subtract(despesa));
                })
                .toList();
    }

    /**
     * Resolve os vínculos opcionais: pedido inexistente → 404; cliente
     * omitido com pedido informado herda o cliente do pedido.
     */
    private void vincularReferencias(FinancialTransaction transacao, Long pedidoId,
                                     Long clienteId) {
        Order pedido = null;
        if (pedidoId != null) {
            pedido = orderRepository.findById(pedidoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));
        }
        transacao.setPedido(pedido);

        if (clienteId != null) {
            transacao.setCliente(clientRepository.findById(clienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId)));
        } else {
            transacao.setCliente(pedido != null ? pedido.getCliente() : null);
        }
    }

    private BigDecimal total(Map<TransactionType, Map<TransactionStatus, BigDecimal>> totais,
                             TransactionType tipo, TransactionStatus status) {
        return totais.getOrDefault(tipo, Map.of()).getOrDefault(status, BigDecimal.ZERO);
    }

    private FinancialTransaction obterTransacao(Long id) {
        return financialTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação financeira", id));
    }

    private FinancialTransaction obterTransacaoDetalhada(Long id) {
        return financialTransactionRepository.findDetalhadaById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação financeira", id));
    }
}
