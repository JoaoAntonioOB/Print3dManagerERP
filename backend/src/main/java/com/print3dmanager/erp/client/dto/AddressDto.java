package com.print3dmanager.erp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Endereço do cliente")
public record AddressDto(

        @Size(max = 160, message = "O logradouro deve ter no máximo 160 caracteres")
        String logradouro,

        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 80, message = "O complemento deve ter no máximo 80 caracteres")
        String complemento,

        @Size(max = 80, message = "O bairro deve ter no máximo 80 caracteres")
        String bairro,

        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres")
        String cidade,

        @Schema(example = "PR")
        @Pattern(regexp = "[A-Z]{2}", message = "O estado deve ser a UF com 2 letras maiúsculas")
        String estado,

        @Schema(example = "85501-000")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido (formato 00000-000)")
        String cep
) {
}
