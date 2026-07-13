package com.print3dmanager.erp.order.model;

/**
 * Resultado de um job de impressão
 * (CHECK de historico_impressoes.status — migração V8).
 */
public enum PrintStatus {
    EM_ANDAMENTO,
    CONCLUIDA,
    FALHOU,
    CANCELADA
}
