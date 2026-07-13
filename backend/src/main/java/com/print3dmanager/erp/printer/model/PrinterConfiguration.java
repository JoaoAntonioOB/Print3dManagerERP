package com.print3dmanager.erp.printer.model;

import com.print3dmanager.erp.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Parâmetros de custo usados no cálculo de orçamentos
 * (tabela configuracoes_impressora — migração V3).
 * Quando {@code impressora} é null, trata-se da configuração padrão global.
 */
@Entity
@Table(name = "configuracoes_impressora")
@Getter
@Setter
@NoArgsConstructor
public class PrinterConfiguration extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impressora_id", unique = true)
    private Printer impressora;

    @Column(name = "valor_kwh", nullable = false, precision = 10, scale = 4)
    private BigDecimal valorKwh;

    @Column(name = "valor_hora_maquina", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorHoraMaquina;

    @Column(name = "custo_desgaste_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoDesgasteHora = BigDecimal.ZERO;

    /** Margem de lucro padrão em porcentagem (ex.: 100.00 = 100%). */
    @Column(name = "markup_padrao", nullable = false, precision = 5, scale = 2)
    private BigDecimal markupPadrao;
}
