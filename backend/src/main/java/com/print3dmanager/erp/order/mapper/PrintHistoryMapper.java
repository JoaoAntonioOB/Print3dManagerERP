package com.print3dmanager.erp.order.mapper;

import com.print3dmanager.erp.order.dto.PrintHistoryResponse;
import com.print3dmanager.erp.order.model.PrintHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversão entidade → DTO do histórico de impressões.
 */
@Mapper
public interface PrintHistoryMapper {

    @Mapping(target = "impressoraId", source = "impressora.id")
    @Mapping(target = "impressoraNome", source = "impressora.nome")
    @Mapping(target = "filamentoId", source = "filamento.id")
    @Mapping(target = "filamentoNome", source = "filamento.nome")
    @Mapping(target = "itemPedidoId", source = "itemPedido.id")
    @Mapping(target = "nomePeca", source = "itemPedido.nomePeca")
    @Mapping(target = "pedidoNumero", source = "itemPedido.pedido.numero")
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "usuarioNome", source = "usuario.nome")
    PrintHistoryResponse toResponse(PrintHistory print);
}
