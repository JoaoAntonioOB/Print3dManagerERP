package com.print3dmanager.erp.financial.service;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.client.repository.ClientRepository;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.financial.dto.FinancialSummaryResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionCreateRequest;
import com.print3dmanager.erp.financial.dto.FinancialTransactionResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionUpdateRequest;
import com.print3dmanager.erp.financial.mapper.FinancialTransactionMapper;
import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import com.print3dmanager.erp.financial.repository.FinancialQueryRepository;
import com.print3dmanager.erp.financial.repository.FinancialTransactionRepository;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio de {@link FinancialTransactionService}: rejeição de
 * cadastro já CANCELADO, vínculo (herança de cliente do pedido, pedido
 * inexistente → 404), edição restrita a PENDENTE, máquina de estados do
 * ciclo de vida e exclusão restrita a PENDENTE.
 */
@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceTest {

    @Mock
    private FinancialTransactionRepository financialTransactionRepository;
    @Mock
    private FinancialQueryRepository financialQueryRepository;
    @Mock
    private FinancialTransactionMapper financialTransactionMapper;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private FinancialTransactionService service;

    // ===== criar =====

    @Test
    @DisplayName("criar: status CANCELADA no cadastro é rejeitado")
    void criarRejeitaStatusCancelada() {
        FinancialTransactionCreateRequest request = criarRequest(TransactionStatus.CANCELADA,
                null, null);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CANCELADA");
        verify(financialTransactionRepository, never()).save(any(FinancialTransaction.class));
    }

    @Test
    @DisplayName("criar: status omitido mantém o PENDENTE default da entidade")
    void criarSemStatusMantemDefault() {
        FinancialTransactionCreateRequest request = criarRequest(null, null, null);
        FinancialTransaction entidade = new FinancialTransaction();
        when(financialTransactionMapper.toEntity(request)).thenReturn(entidade);
        when(financialTransactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(financialTransactionMapper.toResponse(any(FinancialTransaction.class)))
                .thenReturn(resposta());

        service.criar(request);

        assertThat(entidade.getStatus()).isEqualTo(TransactionStatus.PENDENTE);
    }

    @Test
    @DisplayName("criar: pedido inexistente → 404, nada é salvo")
    void criarComPedidoInexistente() {
        FinancialTransactionCreateRequest request = criarRequest(null, 5L, null);
        when(financialTransactionMapper.toEntity(request)).thenReturn(new FinancialTransaction());
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(financialTransactionRepository, never()).save(any(FinancialTransaction.class));
    }

    @Test
    @DisplayName("criar: cliente omitido com pedido informado herda o cliente do pedido")
    void criarHerdaClienteDoPedido() {
        Client cliente = new Client();
        cliente.setId(3L);
        Order pedido = new Order();
        pedido.setId(5L);
        pedido.setCliente(cliente);

        FinancialTransactionCreateRequest request = criarRequest(null, 5L, null);
        FinancialTransaction entidade = new FinancialTransaction();
        when(financialTransactionMapper.toEntity(request)).thenReturn(entidade);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(financialTransactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(financialTransactionMapper.toResponse(any(FinancialTransaction.class)))
                .thenReturn(resposta());

        service.criar(request);

        assertThat(entidade.getCliente()).isSameAs(cliente);
        assertThat(entidade.getPedido()).isSameAs(pedido);
    }

    @Test
    @DisplayName("criar: cliente explícito inexistente → 404")
    void criarComClienteInexistente() {
        FinancialTransactionCreateRequest request = criarRequest(null, null, 9L);
        when(financialTransactionMapper.toEntity(request)).thenReturn(new FinancialTransaction());
        when(clientRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(financialTransactionRepository, never()).save(any(FinancialTransaction.class));
    }

    // ===== atualizar =====

    @Test
    @DisplayName("atualizar: transação não PENDENTE é rejeitada")
    void atualizarRejeitaForaDePendente() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PAGA);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> service.atualizar(1L, atualizarRequest(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDENTES");
    }

    @Test
    @DisplayName("atualizar: transação PENDENTE é editável e revincula referências")
    void atualizarTransacaoPendente() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PENDENTE);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));
        when(financialTransactionMapper.toResponse(transacao)).thenReturn(resposta());

        FinancialTransactionUpdateRequest request = atualizarRequest(null, null);
        service.atualizar(1L, request);

        verify(financialTransactionMapper).atualizar(transacao, request);
        assertThat(transacao.getPedido()).isNull();
        assertThat(transacao.getCliente()).isNull();
    }

    @Test
    @DisplayName("atualizar: id inexistente → 404")
    void atualizarComIdInexistente() {
        when(financialTransactionRepository.findDetalhadaById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(1L, atualizarRequest(null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== alterarStatus (máquina de estados) =====

    @Test
    @DisplayName("alterarStatus: PENDENTE → PAGA é permitida")
    void pendenteParaPaga() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PENDENTE);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));
        when(financialTransactionMapper.toResponse(transacao)).thenReturn(resposta());

        service.alterarStatus(1L, TransactionStatus.PAGA);

        assertThat(transacao.getStatus()).isEqualTo(TransactionStatus.PAGA);
    }

    @Test
    @DisplayName("alterarStatus: PENDENTE → CANCELADA é permitida")
    void pendenteParaCancelada() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PENDENTE);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));
        when(financialTransactionMapper.toResponse(transacao)).thenReturn(resposta());

        service.alterarStatus(1L, TransactionStatus.CANCELADA);

        assertThat(transacao.getStatus()).isEqualTo(TransactionStatus.CANCELADA);
    }

    @Test
    @DisplayName("alterarStatus: PAGA → PENDENTE (estorno) é permitida")
    void pagaParaPendente() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PAGA);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));
        when(financialTransactionMapper.toResponse(transacao)).thenReturn(resposta());

        service.alterarStatus(1L, TransactionStatus.PENDENTE);

        assertThat(transacao.getStatus()).isEqualTo(TransactionStatus.PENDENTE);
    }

    @Test
    @DisplayName("alterarStatus: PAGA → CANCELADA é rejeitada (não há transição direta)")
    void pagaParaCanceladaRejeitada() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PAGA);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> service.alterarStatus(1L, TransactionStatus.CANCELADA))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    @DisplayName("alterarStatus: CANCELADA é terminal — qualquer transição é rejeitada")
    void canceladaETerminal() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.CANCELADA);
        when(financialTransactionRepository.findDetalhadaById(1L))
                .thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> service.alterarStatus(1L, TransactionStatus.PAGA))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.alterarStatus(1L, TransactionStatus.PENDENTE))
                .isInstanceOf(BusinessException.class);
    }

    // ===== excluir =====

    @Test
    @DisplayName("excluir: transação PENDENTE é removida")
    void excluirTransacaoPendente() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PENDENTE);
        when(financialTransactionRepository.findById(1L)).thenReturn(Optional.of(transacao));

        service.excluir(1L);

        verify(financialTransactionRepository).delete(transacao);
    }

    @Test
    @DisplayName("excluir: transação PAGA é preservada como histórico (rejeitada)")
    void excluirTransacaoPagaRejeitada() {
        FinancialTransaction transacao = transacao(1L, TransactionStatus.PAGA);
        when(financialTransactionRepository.findById(1L)).thenReturn(Optional.of(transacao));

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDENTES");
        verify(financialTransactionRepository, never()).delete(any(FinancialTransaction.class));
    }

    @Test
    @DisplayName("excluir: id inexistente → 404")
    void excluirComIdInexistente() {
        when(financialTransactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(financialTransactionRepository, never()).delete(any(FinancialTransaction.class));
    }

    // ===== resumo =====

    @Test
    @DisplayName("resumo: início posterior ao fim é rejeitado")
    void resumoRejeitaPeriodoInvertido() {
        assertThatThrownBy(() -> service.resumo(LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("resumo: sem período informado usa o mês corrente (UTC)")
    void resumoUsaMesCorrenteQuandoOmitido() {
        when(financialQueryRepository.totaisPorTipoEStatus(any(), any())).thenReturn(List.of());

        FinancialSummaryResponse resumo = service.resumo(null, null);

        YearMonth mesAtual = YearMonth.now(ZoneOffset.UTC);
        assertThat(resumo.de()).isEqualTo(mesAtual.atDay(1));
        assertThat(resumo.ate()).isEqualTo(mesAtual.atEndOfMonth());
        assertThat(resumo.receitasPagas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.saldoRealizado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("resumo: agrega receitas e despesas por status, ignorando CANCELADAS "
            + "(a consulta nem as devolve)")
    void resumoAgregaPorTipoEStatus() {
        Object[] receitaPaga = {"RECEITA", "PAGA", new BigDecimal("500.00")};
        Object[] receitaPendente = {"RECEITA", "PENDENTE", new BigDecimal("100.00")};
        Object[] despesaPaga = {"DESPESA", "PAGA", new BigDecimal("200.00")};
        when(financialQueryRepository.totaisPorTipoEStatus(any(), any()))
                .thenReturn(List.of(receitaPaga, receitaPendente, despesaPaga));

        FinancialSummaryResponse resumo = service.resumo(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31));

        assertThat(resumo.receitasPagas()).isEqualByComparingTo("500.00");
        assertThat(resumo.receitasPendentes()).isEqualByComparingTo("100.00");
        assertThat(resumo.despesasPagas()).isEqualByComparingTo("200.00");
        assertThat(resumo.despesasPendentes()).isEqualByComparingTo(BigDecimal.ZERO);
        // saldo realizado = receitasPagas - despesasPagas
        assertThat(resumo.saldoRealizado()).isEqualByComparingTo("300.00");
        // saldo previsto = (500+100) - (200+0)
        assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("400.00");
    }

    // ===== resumoMensal =====

    @Test
    @DisplayName("resumoMensal: quantidade acima do máximo é limitada a 60 meses")
    void resumoMensalLimitaMaximo() {
        when(financialQueryRepository.movimentoPagoPorMes(any())).thenReturn(List.of());

        assertThat(service.resumoMensal(999)).hasSize(60);
    }

    @Test
    @DisplayName("resumoMensal: quantidade abaixo do mínimo é elevada a 1 mês")
    void resumoMensalLimitaMinimo() {
        when(financialQueryRepository.movimentoPagoPorMes(any())).thenReturn(List.of());

        assertThat(service.resumoMensal(0)).hasSize(1);
    }

    // ===== helpers =====

    private FinancialTransactionCreateRequest criarRequest(TransactionStatus status,
                                                            Long pedidoId, Long clienteId) {
        return new FinancialTransactionCreateRequest(TransactionType.RECEITA, "Vendas",
                "Descrição", new BigDecimal("100.00"), LocalDate.now(), status, "PIX",
                pedidoId, clienteId, null);
    }

    private FinancialTransactionUpdateRequest atualizarRequest(Long pedidoId, Long clienteId) {
        return new FinancialTransactionUpdateRequest(TransactionType.RECEITA, "Vendas",
                "Descrição", new BigDecimal("100.00"), LocalDate.now(), "PIX",
                pedidoId, clienteId, null);
    }

    private FinancialTransaction transacao(Long id, TransactionStatus status) {
        FinancialTransaction transacao = new FinancialTransaction();
        transacao.setId(id);
        transacao.setStatus(status);
        transacao.setValor(new BigDecimal("100.00"));
        return transacao;
    }

    private FinancialTransactionResponse resposta() {
        return new FinancialTransactionResponse(1L, TransactionType.RECEITA, "Vendas",
                "Descrição", new BigDecimal("100.00"), LocalDate.now(), TransactionStatus.PENDENTE,
                "PIX", null, null, null, null, null, null, null);
    }
}
