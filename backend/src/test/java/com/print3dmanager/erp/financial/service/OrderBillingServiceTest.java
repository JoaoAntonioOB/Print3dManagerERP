package com.print3dmanager.erp.financial.service;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceConflictException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.financial.mapper.FinancialTransactionMapper;
import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import com.print3dmanager.erp.financial.repository.FinancialTransactionRepository;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderBillingServiceTest {

    @Mock
    private FinancialTransactionRepository financialTransactionRepository;
    @Mock
    private FinancialTransactionMapper financialTransactionMapper;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderBillingService service;

    @Test
    @DisplayName("faturar: pedido CONCLUIDO gera receita PENDENTE vinculada ao pedido e ao cliente")
    void faturarGeraReceitaPendente() {
        Order pedido = pedido(OrderStatus.CONCLUIDO, "350.00");
        when(orderRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                5L, TransactionType.RECEITA, TransactionStatus.CANCELADA)).thenReturn(false);
        when(financialTransactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.faturar(5L);

        ArgumentCaptor<FinancialTransaction> captor =
                ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(financialTransactionRepository).save(captor.capture());
        FinancialTransaction receita = captor.getValue();
        assertThat(receita.getTipo()).isEqualTo(TransactionType.RECEITA);
        assertThat(receita.getStatus()).isEqualTo(TransactionStatus.PENDENTE);
        assertThat(receita.getCategoria()).isEqualTo("Vendas");
        assertThat(receita.getValor()).isEqualByComparingTo("350.00");
        assertThat(receita.getPedido()).isSameAs(pedido);
        assertThat(receita.getCliente()).isSameAs(pedido.getCliente());
        // sem data de entrega realizada, a data da transação é hoje
        assertThat(receita.getDataTransacao()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("faturar: a data da transação usa a data de entrega quando existente")
    void faturarUsaDataDeEntrega() {
        Order pedido = pedido(OrderStatus.ENTREGUE, "100.00");
        pedido.setDataEntregaRealizada(LocalDate.of(2026, 6, 10));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                any(), any(), any())).thenReturn(false);
        when(financialTransactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.faturar(5L);

        ArgumentCaptor<FinancialTransaction> captor =
                ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(financialTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDataTransacao()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    @DisplayName("faturar: pedido fora de CONCLUIDO/ENTREGUE é rejeitado")
    void faturarExigeStatusFaturavel() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(pedido(OrderStatus.EM_PRODUCAO, "100.00")));

        assertThatThrownBy(() -> service.faturar(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CONCLUIDOS ou ENTREGUES");
    }

    @Test
    @DisplayName("faturar: pedido já faturado responde conflito (409)")
    void faturarDuplicadoConflita() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(pedido(OrderStatus.CONCLUIDO, "100.00")));
        when(financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                5L, TransactionType.RECEITA, TransactionStatus.CANCELADA)).thenReturn(true);

        assertThatThrownBy(() -> service.faturar(5L))
                .isInstanceOf(ResourceConflictException.class);
        verify(financialTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("faturar: pedido sem valor não gera receita")
    void faturarSemValorRejeitado() {
        when(orderRepository.findById(5L))
                .thenReturn(Optional.of(pedido(OrderStatus.CONCLUIDO, "0.00")));
        when(financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.faturar(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não tem valor");
    }

    @Test
    @DisplayName("faturar: pedido inexistente → 404")
    void faturarPedidoInexistente() {
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.faturar(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("entrega automática: silenciosa quando o pedido já foi faturado")
    void geracaoAutomaticaSilenciosaQuandoJaFaturado() {
        Order pedido = pedido(OrderStatus.ENTREGUE, "100.00");
        when(financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                5L, TransactionType.RECEITA, TransactionStatus.CANCELADA)).thenReturn(true);

        service.gerarReceitaSeNecessario(pedido);

        verify(financialTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("entrega automática: silenciosa quando o pedido não tem valor")
    void geracaoAutomaticaSilenciosaSemValor() {
        service.gerarReceitaSeNecessario(pedido(OrderStatus.ENTREGUE, "0.00"));

        verify(financialTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("entrega automática: gera a receita quando ainda não faturado")
    void geracaoAutomaticaCriaReceita() {
        Order pedido = pedido(OrderStatus.ENTREGUE, "200.00");
        when(financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                5L, TransactionType.RECEITA, TransactionStatus.CANCELADA)).thenReturn(false);
        when(financialTransactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.gerarReceitaSeNecessario(pedido);

        ArgumentCaptor<FinancialTransaction> captor =
                ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(financialTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("200.00");
    }

    private Order pedido(OrderStatus status, String valorTotal) {
        Client cliente = new Client();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");

        Order pedido = new Order();
        pedido.setId(5L);
        pedido.setNumero("PED-2026-0001");
        pedido.setStatus(status);
        pedido.setValorTotal(new BigDecimal(valorTotal));
        pedido.setCliente(cliente);
        return pedido;
    }
}
