package com.print3dmanager.erp.client.model;

import com.print3dmanager.erp.common.model.BaseEntity;
import com.print3dmanager.erp.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cliente pessoa física ou jurídica (tabela clientes — migração V2).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Client extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(length = 160)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 10)
    private PersonType tipoPessoa = PersonType.FISICA;

    @Column(name = "cpf_cnpj", length = 18, unique = true)
    private String cpfCnpj;

    @Embedded
    private Address endereco;

    private String observacoes;

    /** Usuário com role CLIENTE vinculado (acesso ao portal de orçamentos). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private User usuario;

    @Column(nullable = false)
    private boolean ativo = true;
}
