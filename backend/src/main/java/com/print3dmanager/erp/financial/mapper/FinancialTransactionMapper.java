package com.print3dmanager.erp.financial.mapper;

import com.print3dmanager.erp.financial.dto.FinancialTransactionCreateRequest;
import com.print3dmanager.erp.financial.dto.FinancialTransactionResponse;
import com.print3dmanager.erp.financial.dto.FinancialTransactionUpdateRequest;
import com.print3dmanager.erp.financial.model.FinancialTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO de transações financeiras. Status e vínculos
 * (pedido/cliente) são tratados no service — o mapper não os toca.
 */
@Mapper
public interface FinancialTransactionMapper {

    @Mapping(source = "pedido.id", target = "pedidoId")
    @Mapping(source = "pedido.numero", target = "pedidoNumero")
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.nome", target = "clienteNome")
    FinancialTransactionResponse toResponse(FinancialTransaction transacao);

    @Mapping(target = "status", ignore = true)
    FinancialTransaction toEntity(FinancialTransactionCreateRequest request);

    void atualizar(@MappingTarget FinancialTransaction transacao,
                   FinancialTransactionUpdateRequest request);
}
