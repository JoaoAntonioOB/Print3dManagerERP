package com.print3dmanager.erp.client.mapper;

import com.print3dmanager.erp.client.dto.ClientCreateRequest;
import com.print3dmanager.erp.client.dto.ClientResponse;
import com.print3dmanager.erp.client.dto.ClientUpdateRequest;
import com.print3dmanager.erp.client.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO do cliente. O endereço aninhado
 * (AddressDto ↔ Address embeddable) é mapeado automaticamente
 * pelo MapStruct por correspondência de nomes.
 */
@Mapper
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    Client toEntity(ClientCreateRequest request);

    void atualizar(@MappingTarget Client client, ClientUpdateRequest request);
}
