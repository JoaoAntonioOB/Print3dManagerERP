package com.print3dmanager.erp.filament.model;

import com.print3dmanager.erp.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Filamento/resina com estoque em gramas (tabela filamentos — migração V4).
 * O custo por kg alimenta o cálculo de custo de material dos orçamentos.
 */
@Entity
@Table(name = "filamentos")
@Getter
@Setter
@NoArgsConstructor
public class Filament extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 80)
    private String marca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FilamentMaterial material;

    @Column(length = 40)
    private String cor;

    @Column(name = "diametro_mm", nullable = false, precision = 4, scale = 2)
    private BigDecimal diametroMm = new BigDecimal("1.75");

    @Column(name = "peso_bobina_g", precision = 8, scale = 2)
    private BigDecimal pesoBobinaG;

    @Column(name = "custo_por_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal custoPorKg;

    @Column(name = "quantidade_estoque_g", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidadeEstoqueG = BigDecimal.ZERO;

    @Column(name = "estoque_minimo_g", nullable = false, precision = 12, scale = 2)
    private BigDecimal estoqueMinimoG = BigDecimal.ZERO;

    @Column(name = "temperatura_bico")
    private Integer temperaturaBico;

    @Column(name = "temperatura_mesa")
    private Integer temperaturaMesa;

    @Column(nullable = false)
    private boolean ativo = true;
}
