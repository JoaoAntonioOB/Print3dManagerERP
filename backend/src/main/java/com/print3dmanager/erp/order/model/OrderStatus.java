package com.print3dmanager.erp.order.model;

/**
 * Ciclo de vida do pedido (CHECK de pedidos.status — migração V6).
 */
public enum OrderStatus {
    PENDENTE,
    EM_PRODUCAO,
    CONCLUIDO,
    ENTREGUE,
    CANCELADO
}
