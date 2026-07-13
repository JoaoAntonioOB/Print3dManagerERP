package com.print3dmanager.erp.client.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * Endereço embutido na tabela clientes (colunas achatadas — migração V2).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Column(length = 160)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 80)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    /** UF com 2 letras — coluna CHAR(2) no banco. */
    @JdbcTypeCode(Types.CHAR)
    @Column(length = 2)
    private String estado;

    @Column(length = 9)
    private String cep;
}
