package com.print3dmanager.erp.order.model;

import com.print3dmanager.erp.common.model.BaseEntity;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.user.model.User;
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
import java.time.Instant;

/**
 * Registro de um job de impressão executado
 * (tabela historico_impressoes — migração V8).
 */
@Entity
@Table(name = "historico_impressoes")
@Getter
@Setter
@NoArgsConstructor
public class PrintHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "impressora_id", nullable = false)
    private Printer impressora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filamento_id")
    private Filament filamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_pedido_id")
    private OrderItem itemPedido;

    /** Operador responsável pela impressão. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrintStatus status = PrintStatus.EM_ANDAMENTO;

    @Column(name = "iniciado_em", nullable = false)
    private Instant iniciadoEm;

    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    @Column(name = "tempo_total_minutos")
    private Integer tempoTotalMinutos;

    @Column(name = "peso_utilizado_g", precision = 10, scale = 2)
    private BigDecimal pesoUtilizadoG;

    @Column(name = "consumo_energia_kwh", precision = 10, scale = 3)
    private BigDecimal consumoEnergiaKwh;

    @Column(name = "custo_total", precision = 12, scale = 2)
    private BigDecimal custoTotal;

    @Column(name = "motivo_falha")
    private String motivoFalha;

    private String observacoes;
}
