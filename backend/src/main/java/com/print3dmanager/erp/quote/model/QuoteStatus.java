package com.print3dmanager.erp.quote.model;

/**
 * Ciclo de vida do orçamento (CHECK de orcamentos.status — migração V7).
 */
public enum QuoteStatus {
    RASCUNHO,
    ENVIADO,
    APROVADO,
    REJEITADO,
    EXPIRADO,
    CONVERTIDO
}
