package com.print3dmanager.erp.printer.model;

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
import java.time.LocalDate;

/**
 * Impressora 3D (tabela impressoras — migração V3).
 */
@Entity
@Table(name = "impressoras")
@Getter
@Setter
@NoArgsConstructor
public class Printer extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 80)
    private String marca;

    @Column(length = 80)
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrinterStatus status = PrinterStatus.DISPONIVEL;

    @Column(name = "potencia_watts")
    private Integer potenciaWatts;

    @Column(name = "volume_x_mm")
    private Integer volumeXMm;

    @Column(name = "volume_y_mm")
    private Integer volumeYMm;

    @Column(name = "volume_z_mm")
    private Integer volumeZMm;

    @Column(name = "horas_impressao_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal horasImpressaoTotal = BigDecimal.ZERO;

    @Column(name = "valor_aquisicao", precision = 12, scale = 2)
    private BigDecimal valorAquisicao;

    @Column(name = "data_aquisicao")
    private LocalDate dataAquisicao;

    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;
}
