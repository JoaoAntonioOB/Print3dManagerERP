package com.print3dmanager.erp.inventory.model;

import com.print3dmanager.erp.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Insumo geral do estoque, exceto filamentos
 * (tabela itens_estoque — migração V5).
 */
@Entity
@Table(name = "itens_estoque")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    private String descricao;

    @Column(length = 60)
    private String categoria;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(name = "unidade_medida", nullable = false, length = 10)
    private String unidadeMedida = "UN";

    @Column(name = "quantidade_minima", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeMinima = BigDecimal.ZERO;

    @Column(name = "custo_unitario", precision = 12, scale = 2)
    private BigDecimal custoUnitario;

    @Column(length = 120)
    private String localizacao;

    @Column(nullable = false)
    private boolean ativo = true;
}
