package com.print3dmanager.erp.printer.model;

/**
 * Situação operacional da impressora (CHECK de impressoras.status — migração V3).
 */
public enum PrinterStatus {
    DISPONIVEL,
    IMPRIMINDO,
    EM_MANUTENCAO,
    INATIVA
}
