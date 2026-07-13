package com.print3dmanager.erp.order.mapper;

import com.print3dmanager.erp.order.dto.OrderItemRequest;
import com.print3dmanager.erp.order.dto.OrderItemResponse;
import com.print3dmanager.erp.order.dto.OrderResponse;
import com.print3dmanager.erp.order.dto.OrderSummaryResponse;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Conversões entidade ↔ DTO de pedidos e itens. O filamento dos itens é
 * resolvido no service (o request traz só o id); id/pedido/arquivoModelo
 * nunca vêm do request.
 */
@Mapper
public interface OrderMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "usuarioNome", source = "usuario.nome")
    OrderResponse toResponse(Order order);

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNome", source = "cliente.nome")
    OrderSummaryResponse toSummaryResponse(Order order);

    @Mapping(target = "filamentoId", source = "filamento.id")
    @Mapping(target = "filamentoNome", source = "filamento.nome")
    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantidade", source = "quantidade", defaultValue = "1")
    OrderItem toItemEntity(OrderItemRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantidade", source = "quantidade", defaultValue = "1")
    void atualizarItem(@MappingTarget OrderItem item, OrderItemRequest request);
}
