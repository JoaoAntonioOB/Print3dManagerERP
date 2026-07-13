package com.print3dmanager.erp.order.model;

import com.print3dmanager.erp.common.model.BaseEntity;
import com.print3dmanager.erp.filament.model.Filament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Item (peça) de um pedido (tabela itens_pedido — migração V6).
 */
@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Order pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filamento_id")
    private Filament filamento;

    @Column(name = "nome_peca", nullable = false, length = 160)
    private String nomePeca;

    private String descricao;

    @Column(nullable = false)
    private Integer quantidade = 1;

    @Column(name = "peso_estimado_g", precision = 10, scale = 2)
    private BigDecimal pesoEstimadoG;

    @Column(name = "tempo_impressao_minutos")
    private Integer tempoImpressaoMinutos;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    /** Caminho do arquivo STL/3MF no diretório de uploads. */
    @Column(name = "arquivo_modelo", length = 255)
    private String arquivoModelo;

    /** Subtotal do item: quantidade × preço unitário. */
    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
