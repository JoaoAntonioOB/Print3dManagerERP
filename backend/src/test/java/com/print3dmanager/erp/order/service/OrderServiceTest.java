package com.print3dmanager.erp.order.service;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.client.repository.ClientRepository;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.financial.service.OrderBillingService;
import com.print3dmanager.erp.order.dto.OrderCreateRequest;
import com.print3dmanager.erp.order.dto.OrderItemRequest;
import com.print3dmanager.erp.order.dto.OrderUpdateRequest;
import com.print3dmanager.erp.order.mapper.OrderMapper;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderItem;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.repository.OrderRepository;
import com.print3dmanager.erp.user.model.User;
import com.print3dmanager.erp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private FilamentRepository filamentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderBillingService orderBillingService;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService service;

    private final int ano = Year.now().getValue();

    // ===== criação =====

    @Test
    @DisplayName("criar: primeiro pedido do ano recebe PED-<ano>-0001 e total = Σ itens − desconto")
    void criarGeraNumeroInicialECalculaTotal() {
        stubClienteAtivo(1L);
        when(userRepository.getReferenceById(9L)).thenReturn(usuario(9L));
        when(orderRepository.findTopByNumeroStartingWithOrderByIdDesc(anyString()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        stubMapperDeItens();

        OrderCreateRequest request = new OrderCreateRequest(1L, null, new BigDecimal("10.00"),
                null, List.of(
                item(null, "Peça A", 2, "50.00"),
                item(null, "Peça B", null, "30.00")));

        service.criar(request, 9L);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order pedido = captor.getValue();
        assertThat(pedido.getNumero()).isEqualTo("PED-%d-0001".formatted(ano));
        // 2 × 50,00 + 1 × 30,00 − 10,00 = 120,00
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("120.00");
        assertThat(pedido.getStatus()).isEqualTo(OrderStatus.PENDENTE);
        assertThat(pedido.getItens()).hasSize(2);
    }

    @Test
    @DisplayName("criar: número continua a sequência do último pedido do ano")
    void criarContinuaSequenciaDoAno() {
        stubClienteAtivo(1L);
        when(userRepository.getReferenceById(9L)).thenReturn(usuario(9L));
        Order ultimo = new Order();
        ultimo.setNumero("PED-%d-0042".formatted(ano));
        when(orderRepository.findTopByNumeroStartingWithOrderByIdDesc("PED-%d-".formatted(ano)))
                .thenReturn(Optional.of(ultimo));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        stubMapperDeItens();

        service.criar(pedidoSimples(), 9L);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getNumero()).isEqualTo("PED-%d-0043".formatted(ano));
    }

    @Test
    @DisplayName("criar: desconto maior que o subtotal é rejeitado")
    void criarRejeitaDescontoMaiorQueSubtotal() {
        stubClienteAtivo(1L);
        when(userRepository.getReferenceById(9L)).thenReturn(usuario(9L));
        when(orderRepository.findTopByNumeroStartingWithOrderByIdDesc(anyString()))
                .thenReturn(Optional.empty());
        stubMapperDeItens();

        OrderCreateRequest request = new OrderCreateRequest(1L, null, new BigDecimal("999.00"),
                null, List.of(item(null, "Peça A", 1, "50.00")));

        assertThatThrownBy(() -> service.criar(request, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desconto");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar: cliente desativado não pode receber pedidos")
    void criarRejeitaClienteDesativado() {
        Client cliente = cliente(1L);
        cliente.setAtivo(false);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.criar(pedidoSimples(), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    @DisplayName("criar: cliente inexistente → 404")
    void criarClienteInexistente() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(pedidoSimples(), 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== máquina de estados =====

    @Test
    @DisplayName("status: PENDENTE → EM_PRODUCAO é permitido")
    void transicaoValida() {
        Order pedido = pedidoComStatus(OrderStatus.PENDENTE);
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));

        service.alterarStatus(5L, OrderStatus.EM_PRODUCAO);

        assertThat(pedido.getStatus()).isEqualTo(OrderStatus.EM_PRODUCAO);
    }

    @Test
    @DisplayName("status: PENDENTE → CONCLUIDO é rejeitado (pula etapa)")
    void transicaoInvalidaRejeitada() {
        Order pedido = pedidoComStatus(OrderStatus.PENDENTE);
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.alterarStatus(5L, OrderStatus.CONCLUIDO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
        assertThat(pedido.getStatus()).isEqualTo(OrderStatus.PENDENTE);
    }

    @Test
    @DisplayName("status: ENTREGUE e CANCELADO são estados terminais")
    void estadosTerminaisNaoTransicionam() {
        for (OrderStatus terminal : List.of(OrderStatus.ENTREGUE, OrderStatus.CANCELADO)) {
            Order pedido = pedidoComStatus(terminal);
            when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));

            assertThatThrownBy(() -> service.alterarStatus(5L, OrderStatus.PENDENTE))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("status: entrega grava a data realizada e dispara o faturamento automático")
    void entregaGravaDataEFatura() {
        Order pedido = pedidoComStatus(OrderStatus.CONCLUIDO);
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));

        service.alterarStatus(5L, OrderStatus.ENTREGUE);

        assertThat(pedido.getDataEntregaRealizada()).isEqualTo(LocalDate.now());
        verify(orderBillingService).gerarReceitaSeNecessario(pedido);
    }

    @Test
    @DisplayName("status: data de entrega já preenchida é preservada")
    void entregaPreservaDataExistente() {
        Order pedido = pedidoComStatus(OrderStatus.CONCLUIDO);
        LocalDate dataOriginal = LocalDate.of(2026, 1, 15);
        pedido.setDataEntregaRealizada(dataOriginal);
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));

        service.alterarStatus(5L, OrderStatus.ENTREGUE);

        assertThat(pedido.getDataEntregaRealizada()).isEqualTo(dataOriginal);
    }

    // ===== atualização e mescla de itens =====

    @Test
    @DisplayName("atualizar: somente pedidos PENDENTES podem ser editados")
    void atualizarSomentePendente() {
        Order pedido = pedidoComStatus(OrderStatus.EM_PRODUCAO);
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.atualizar(5L, atualizacaoSimples()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDENTES");
    }

    @Test
    @DisplayName("atualizar: com id atualiza, sem id cria, ausente da lista remove")
    void atualizarMesclaItens() {
        Order pedido = pedidoComStatus(OrderStatus.PENDENTE);
        OrderItem mantido = itemExistente(1L, "Peça mantida", 1, "10.00");
        OrderItem removido = itemExistente(2L, "Peça removida", 1, "99.00");
        pedido.adicionarItem(mantido);
        pedido.adicionarItem(removido);
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));
        stubClienteAtivo(1L);
        stubMapperDeItens();
        stubMapperDeAtualizacao();

        OrderUpdateRequest request = new OrderUpdateRequest(1L, null, BigDecimal.ZERO, null,
                List.of(
                        item(1L, "Peça mantida editada", 3, "20.00"),
                        item(null, "Peça nova", 1, "5.00")));

        service.atualizar(5L, request);

        assertThat(pedido.getItens()).hasSize(2);
        assertThat(mantido.getNomePeca()).isEqualTo("Peça mantida editada");
        assertThat(mantido.getQuantidade()).isEqualTo(3);
        // 3 × 20,00 + 1 × 5,00 = 65,00
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("65.00");
        assertThat(pedido.getItens()).noneMatch(item -> "Peça removida".equals(item.getNomePeca()));
    }

    @Test
    @DisplayName("atualizar: item com id de outro pedido é rejeitado")
    void atualizarRejeitaItemDeOutroPedido() {
        Order pedido = pedidoComStatus(OrderStatus.PENDENTE);
        pedido.adicionarItem(itemExistente(1L, "Peça", 1, "10.00"));
        when(orderRepository.findDetalhadoById(5L)).thenReturn(Optional.of(pedido));
        stubClienteAtivo(1L);

        OrderUpdateRequest request = new OrderUpdateRequest(1L, null, BigDecimal.ZERO, null,
                List.of(item(99L, "Peça alheia", 1, "10.00")));

        assertThatThrownBy(() -> service.atualizar(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pertence");
    }

    // ===== exclusão =====

    @Test
    @DisplayName("excluir: pedido PENDENTE é removido fisicamente")
    void excluirPendente() {
        Order pedido = pedidoComStatus(OrderStatus.PENDENTE);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(pedido));

        service.excluir(5L);

        verify(orderRepository).delete(pedido);
    }

    @Test
    @DisplayName("excluir: fora de PENDENTE deve-se cancelar, não excluir")
    void excluirSomentePendente() {
        Order pedido = pedidoComStatus(OrderStatus.ENTREGUE);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.excluir(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancele");
        verify(orderRepository, never()).delete(any(Order.class));
    }

    // ===== fixtures =====

    private Client cliente(Long id) {
        Client cliente = new Client();
        cliente.setId(id);
        cliente.setNome("Cliente Teste");
        cliente.setAtivo(true);
        return cliente;
    }

    private User usuario(Long id) {
        User usuario = new User();
        usuario.setId(id);
        usuario.setNome("Operador Teste");
        return usuario;
    }

    private Order pedidoComStatus(OrderStatus status) {
        Order pedido = new Order();
        pedido.setId(5L);
        pedido.setNumero("PED-%d-0001".formatted(ano));
        pedido.setCliente(cliente(1L));
        pedido.setStatus(status);
        pedido.setDesconto(BigDecimal.ZERO);
        return pedido;
    }

    private OrderItem itemExistente(Long id, String nomePeca, int quantidade, String preco) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setNomePeca(nomePeca);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(new BigDecimal(preco));
        return item;
    }

    private OrderItemRequest item(Long id, String nomePeca, Integer quantidade, String preco) {
        return new OrderItemRequest(id, null, nomePeca, null, quantidade,
                null, null, new BigDecimal(preco));
    }

    private OrderCreateRequest pedidoSimples() {
        return new OrderCreateRequest(1L, null, null, null,
                List.of(item(null, "Peça A", 1, "50.00")));
    }

    private OrderUpdateRequest atualizacaoSimples() {
        return new OrderUpdateRequest(1L, null, BigDecimal.ZERO, null,
                List.of(item(null, "Peça A", 1, "50.00")));
    }

    private void stubClienteAtivo(Long id) {
        when(clientRepository.findById(id)).thenReturn(Optional.of(cliente(id)));
    }

    /** Reproduz o toItemEntity do MapStruct (quantidade omitida assume 1). */
    private void stubMapperDeItens() {
        when(orderMapper.toItemEntity(any(OrderItemRequest.class))).thenAnswer(inv -> {
            OrderItemRequest request = inv.getArgument(0);
            OrderItem item = new OrderItem();
            item.setNomePeca(request.nomePeca());
            item.setQuantidade(request.quantidade() == null ? 1 : request.quantidade());
            item.setPrecoUnitario(request.precoUnitario());
            return item;
        });
    }

    /** Reproduz o atualizarItem do MapStruct sobre o item existente. */
    private void stubMapperDeAtualizacao() {
        org.mockito.Mockito.doAnswer(inv -> {
            OrderItem item = inv.getArgument(0);
            OrderItemRequest request = inv.getArgument(1);
            item.setNomePeca(request.nomePeca());
            item.setQuantidade(request.quantidade() == null ? 1 : request.quantidade());
            item.setPrecoUnitario(request.precoUnitario());
            return null;
        }).when(orderMapper).atualizarItem(any(OrderItem.class), any(OrderItemRequest.class));
    }
}
