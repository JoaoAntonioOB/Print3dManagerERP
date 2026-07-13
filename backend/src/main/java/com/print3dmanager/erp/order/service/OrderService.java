package com.print3dmanager.erp.order.service;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.client.repository.ClientRepository;
import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.order.dto.OrderCreateRequest;
import com.print3dmanager.erp.order.dto.OrderItemRequest;
import com.print3dmanager.erp.order.dto.OrderResponse;
import com.print3dmanager.erp.order.dto.OrderSummaryResponse;
import com.print3dmanager.erp.order.dto.OrderUpdateRequest;
import com.print3dmanager.erp.order.mapper.OrderMapper;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderItem;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.repository.OrderRepository;
import com.print3dmanager.erp.order.repository.OrderSpecifications;
import com.print3dmanager.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regras de negócio de pedidos: número sequencial por ano, itens geridos
 * junto com o pedido (substituição por lista), total calculado dos itens
 * e ciclo de vida controlado por máquina de estados.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    /** Transições de status permitidas a partir de cada estado. */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSICOES = Map.of(
            OrderStatus.PENDENTE, Set.of(OrderStatus.EM_PRODUCAO, OrderStatus.CANCELADO),
            OrderStatus.EM_PRODUCAO, Set.of(OrderStatus.CONCLUIDO, OrderStatus.CANCELADO),
            OrderStatus.CONCLUIDO, Set.of(OrderStatus.ENTREGUE),
            OrderStatus.ENTREGUE, Set.of(),
            OrderStatus.CANCELADO, Set.of());

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final FilamentRepository filamentRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listar(String busca, OrderStatus status,
                                                     Long clienteId, Pageable pageable) {
        return PageResponse.de(
                orderRepository.findAll(
                                OrderSpecifications.comFiltros(busca, status, clienteId), pageable)
                        .map(orderMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public OrderResponse buscarPorId(Long id) {
        return orderMapper.toResponse(obterPedidoDetalhado(id));
    }

    @Transactional
    public OrderResponse criar(OrderCreateRequest request, Long usuarioId) {
        Order pedido = new Order();
        pedido.setCliente(obterClienteAtivo(request.clienteId()));
        pedido.setUsuario(userRepository.getReferenceById(usuarioId));
        pedido.setDataEntregaPrevista(request.dataEntregaPrevista());
        pedido.setDesconto(request.desconto() == null ? BigDecimal.ZERO : request.desconto());
        pedido.setObservacoes(request.observacoes());
        pedido.setNumero(gerarNumero());

        request.itens().forEach(itemRequest -> {
            OrderItem item = orderMapper.toItemEntity(itemRequest);
            item.setFilamento(obterFilamentoAtivo(itemRequest.filamentoId()));
            pedido.adicionarItem(item);
        });
        recalcularTotal(pedido);

        return orderMapper.toResponse(orderRepository.save(pedido));
    }

    @Transactional
    public OrderResponse atualizar(Long id, OrderUpdateRequest request) {
        Order pedido = obterPedidoDetalhado(id);
        if (pedido.getStatus() != OrderStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente pedidos PENDENTES podem ser editados. Status atual: %s."
                            .formatted(pedido.getStatus()));
        }

        pedido.setCliente(obterClienteAtivo(request.clienteId()));
        pedido.setDataEntregaPrevista(request.dataEntregaPrevista());
        pedido.setDesconto(request.desconto());
        pedido.setObservacoes(request.observacoes());
        mesclarItens(pedido, request.itens());
        recalcularTotal(pedido);

        return orderMapper.toResponse(pedido);
    }

    @Transactional
    public OrderResponse alterarStatus(Long id, OrderStatus novoStatus) {
        Order pedido = obterPedidoDetalhado(id);
        if (!TRANSICOES.get(pedido.getStatus()).contains(novoStatus)) {
            throw new BusinessException(
                    "Transição de status inválida: %s → %s."
                            .formatted(pedido.getStatus(), novoStatus));
        }
        pedido.setStatus(novoStatus);
        if (novoStatus == OrderStatus.ENTREGUE && pedido.getDataEntregaRealizada() == null) {
            pedido.setDataEntregaRealizada(LocalDate.now());
        }
        return orderMapper.toResponse(pedido);
    }

    /** Exclusão física, permitida apenas para pedidos ainda PENDENTES. */
    @Transactional
    public void excluir(Long id) {
        Order pedido = obterPedido(id);
        if (pedido.getStatus() != OrderStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente pedidos PENDENTES podem ser excluídos — cancele o pedido "
                            + "para preservar o histórico.");
        }
        orderRepository.delete(pedido);
    }

    /**
     * Substitui os itens do pedido pela lista recebida: com id = atualiza,
     * sem id = cria, ausente da lista = remove (orphanRemoval).
     */
    private void mesclarItens(Order pedido, List<OrderItemRequest> requests) {
        Map<Long, OrderItem> atuais = new HashMap<>();
        pedido.getItens().forEach(item -> atuais.put(item.getId(), item));

        Set<Long> idsRecebidos = requests.stream()
                .map(OrderItemRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        pedido.getItens().removeIf(item -> !idsRecebidos.contains(item.getId()));

        requests.forEach(itemRequest -> {
            if (itemRequest.id() == null) {
                OrderItem novo = orderMapper.toItemEntity(itemRequest);
                novo.setFilamento(obterFilamentoAtivo(itemRequest.filamentoId()));
                pedido.adicionarItem(novo);
                return;
            }
            OrderItem existente = atuais.get(itemRequest.id());
            if (existente == null) {
                throw new BusinessException(
                        "O item %d não pertence a este pedido.".formatted(itemRequest.id()));
            }
            orderMapper.atualizarItem(existente, itemRequest);
            existente.setFilamento(obterFilamentoAtivo(itemRequest.filamentoId()));
        });
    }

    /** valorTotal = Σ (quantidade × preço unitário) − desconto (nunca negativo). */
    private void recalcularTotal(Order pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.subtract(pedido.getDesconto());
        if (total.signum() < 0) {
            throw new BusinessException(
                    "O desconto (%s) não pode ser maior que o subtotal dos itens (%s)."
                            .formatted(pedido.getDesconto().toPlainString(),
                                    subtotal.toPlainString()));
        }
        pedido.setValorTotal(total);
    }

    /**
     * Gera o próximo número do ano (PED-2026-0001). O advisory lock do
     * Postgres serializa geradores concorrentes até o fim da transação;
     * a unique constraint em pedidos.numero é o backstop.
     */
    private String gerarNumero() {
        int ano = Year.now().getValue();
        orderRepository.travarGeracaoNumero(ano);
        String prefixo = "PED-%d-".formatted(ano);
        int proximo = orderRepository.findTopByNumeroStartingWithOrderByIdDesc(prefixo)
                .map(ultimo -> Integer.parseInt(
                        ultimo.getNumero().substring(prefixo.length())) + 1)
                .orElse(1);
        return "%s%04d".formatted(prefixo, proximo);
    }

    private Client obterClienteAtivo(Long id) {
        Client cliente = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        if (!cliente.isAtivo()) {
            throw new BusinessException(
                    "O cliente %s está desativado e não pode receber novos pedidos."
                            .formatted(cliente.getNome()));
        }
        return cliente;
    }

    private Filament obterFilamentoAtivo(Long id) {
        if (id == null) {
            return null;
        }
        Filament filamento = filamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filamento", id));
        if (!filamento.isAtivo()) {
            throw new BusinessException(
                    "O filamento %s está desativado e não pode ser usado em novos itens."
                            .formatted(filamento.getNome()));
        }
        return filamento;
    }

    private Order obterPedido(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }

    private Order obterPedidoDetalhado(Long id) {
        return orderRepository.findDetalhadoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }
}
