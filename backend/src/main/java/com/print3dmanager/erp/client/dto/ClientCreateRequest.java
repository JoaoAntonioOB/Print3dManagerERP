package com.print3dmanager.erp.client.dto;

import com.print3dmanager.erp.client.model.PersonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação de cliente")
public record ClientCreateRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 160, message = "O nome deve ter no máximo 160 caracteres")
        String nome,

        @Email(message = "E-mail inválido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        String telefone,

        @NotNull(message = "O tipo de pessoa é obrigatório")
        PersonType tipoPessoa,

        @Schema(description = "CPF ou CNPJ, com ou sem máscara", example = "123.456.789-09")
        @Size(max = 18, message = "O CPF/CNPJ deve ter no máximo 18 caracteres")
        String cpfCnpj,

        @Valid
        AddressDto endereco,

        String observacoes
) {
}
