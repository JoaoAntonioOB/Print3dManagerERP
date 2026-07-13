package com.print3dmanager.erp.user.model;

/**
 * Papéis de acesso do sistema. Os nomes devem coincidir com o
 * CHECK da coluna usuarios.role (migração V1).
 */
public enum Role {
    ADMINISTRADOR,
    OPERADOR,
    FINANCEIRO,
    VISUALIZADOR,
    CLIENTE
}
