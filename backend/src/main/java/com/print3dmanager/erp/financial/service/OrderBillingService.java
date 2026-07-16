package com.print3dmanager.erp.financial.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceConflictException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.financial.dto.FinancialTransactionResponse;
import com.print3dmanager.erp.financial.mapper.FinancialTransactionMapper;
import com.print3dmanager.erp.financial.model.FinancialTransaction;
import com.print3dmanager.erp.financial.model.TransactionStatus;
import com.print3dmanager.erp.financial.model.TransactionType;
import com.print3dmanager.erp.financial.repository.FinancialTransactionRepository;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Faturamento de pedidos: gera a receita PENDENTE vinculada ao pedido e ao
 * cliente. Usado pelo endpoint manual (POST /orders/{id}/faturar) e pela
 * geração automática quando o pedido é marcado como ENTREGUE.
 */
@Service
@RequiredArgsConstructor
public class OrderBillingService {

    private static final String CATEGORIA_VENDAS = "Vendas";

    private final FinancialTransactionRepository financialTransactionRepository;
    private final FinancialTransactionMapper financialTransactionMapper;
    private final OrderRepository orderRepository;

    /**
     * Faturamento manual (antecipado ou não): exige pedido CONCLUIDO ou
     * ENTREGUE; pedido já faturado → 409.
     */
    @Transactional
    public FinancialTransactionResponse faturar(Long pedidoId) {
        Order pedido = orderRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));
        if (pedido.getStatus() != OrderStatus.CONCLUIDO
                && pedido.getStatus() != OrderStatus.ENTREGUE) {
            throw new BusinessException(
                    "Somente pedidos CONCLUIDOS ou ENTREGUES podem ser faturados. "
                            + "Status atual: %s.".formatted(pedido.getStatus()));
        }
        if (jaFaturado(pedidoId)) {
            throw new ResourceConflictException(
                    "O pedido %s já possui uma receita vinculada não cancelada."
                            .formatted(pedido.getNumero()));
        }
        if (pedido.getValorTotal().signum() <= 0) {
            throw new BusinessException(
                    "O pedido %s não tem valor a faturar.".formatted(pedido.getNumero()));
        }
        return financialTransactionMapper.toResponse(
                financialTransactionRepository.save(montarReceita(pedido)));
    }

    /**
     * Geração automática na entrega — silenciosa quando o pedido já foi
     * faturado (ex.: manualmente na conclusão) ou não tem valor.
     */
    @Transactional
    public void gerarReceitaSeNecessario(Order pedido) {
        if (pedido.getValorTotal().signum() <= 0 || jaFaturado(pedido.getId())) {
            return;
        }
        financialTransactionRepository.save(montarReceita(pedido));
    }

    private boolean jaFaturado(Long pedidoId) {
        return financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(
                pedidoId, TransactionType.RECEITA, TransactionStatus.CANCELADA);
    }

    private FinancialTransaction montarReceita(Order pedido) {
        FinancialTransaction receita = new FinancialTransaction();
        receita.setTipo(TransactionType.RECEITA);
        receita.setCategoria(CATEGORIA_VENDAS);
        receita.setDescricao("Receita do pedido %s".formatted(pedido.getNumero()));
        receita.setValor(pedido.getValorTotal());
        receita.setDataTransacao(pedido.getDataEntregaRealizada() != null
                ? pedido.getDataEntregaRealizada() : LocalDate.now());
        receita.setPedido(pedido);
        receita.setCliente(pedido.getCliente());
        return receita;
    }
}
