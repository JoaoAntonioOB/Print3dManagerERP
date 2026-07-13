package com.print3dmanager.erp.client.dto;

import com.print3dmanager.erp.client.model.PersonType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados de um cliente")
public record ClientResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        PersonType tipoPessoa,
        String cpfCnpj,
        AddressDto endereco,
        String observacoes,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
