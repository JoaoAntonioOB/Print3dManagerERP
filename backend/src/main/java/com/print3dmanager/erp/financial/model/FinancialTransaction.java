package com.print3dmanager.erp.financial.model;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.common.model.BaseEntity;
import com.print3dmanager.erp.order.model.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Receita ou despesa do módulo Financeiro
 * (tabela transacoes_financeiras — migração V9).
 * O valor é sempre positivo — o sinal é dado pelo tipo.
 */
@Entity
@Table(name = "transacoes_financeiras")
@Getter
@Setter
@NoArgsConstructor
public class FinancialTransaction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType tipo;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_transacao", nullable = false)
    private LocalDate dataTransacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDENTE;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Order pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Client cliente;

    private String observacoes;
}
