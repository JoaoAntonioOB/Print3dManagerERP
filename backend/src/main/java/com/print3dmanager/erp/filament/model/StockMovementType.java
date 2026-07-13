package com.print3dmanager.erp.filament.model;

/**
 * Tipo de movimentação manual de estoque de filamento:
 * ENTRADA repõe gramas, SAIDA consome (ajustes, perdas, uso fora de pedido).
 */
public enum StockMovementType {
    ENTRADA,
    SAIDA
}
