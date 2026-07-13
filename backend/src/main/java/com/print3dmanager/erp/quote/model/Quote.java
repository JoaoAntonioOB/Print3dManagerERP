package com.print3dmanager.erp.quote.model;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.common.model.BaseEntity;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.order.model.Order;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Orçamento (tabela orcamentos — migração V7).
 * Custos decompostos conforme a regra central de precificação:
 * filamento + energia + hora máquina + desgaste + markup = preço sugerido.
 * O shareToken alimenta o link público de aprovação pelo cliente.
 */
@Entity
@Table(name = "orcamentos")
@Getter
@Setter
@NoArgsConstructor
public class Quote extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Client cliente;

    /** Usuário que elaborou o orçamento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impressora_id")
    private Printer impressora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filamento_id")
    private Filament filamento;

    /** Pedido gerado quando o orçamento é convertido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Order pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status = QuoteStatus.RASCUNHO;

    @Column(name = "share_token", nullable = false, unique = true, updatable = false)
    private UUID shareToken = UUID.randomUUID();

    private String descricao;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "tempo_impressao_minutos")
    private Integer tempoImpressaoMinutos;

    @Column(name = "peso_estimado_g", precision = 10, scale = 2)
    private BigDecimal pesoEstimadoG;

    @Column(name = "custo_filamento", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoFilamento = BigDecimal.ZERO;

    @Column(name = "custo_energia", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoEnergia = BigDecimal.ZERO;

    @Column(name = "custo_hora_maquina", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoHoraMaquina = BigDecimal.ZERO;

    @Column(name = "custo_desgaste", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoDesgaste = BigDecimal.ZERO;

    /** Margem de lucro em porcentagem, editável pelo usuário. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal markup;

    @Column(name = "preco_sugerido", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoSugerido = BigDecimal.ZERO;

    @Column(name = "preco_final", precision = 12, scale = 2)
    private BigDecimal precoFinal;

    @Column(name = "aprovado_em")
    private Instant aprovadoEm;

    private String observacoes;
}
