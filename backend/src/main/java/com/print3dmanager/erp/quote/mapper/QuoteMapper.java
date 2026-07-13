package com.print3dmanager.erp.quote.mapper;

import com.print3dmanager.erp.quote.dto.PublicQuoteResponse;
import com.print3dmanager.erp.quote.dto.QuoteResponse;
import com.print3dmanager.erp.quote.model.Quote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Conversões entidade → DTO de orçamentos. A escrita é montada no service
 * (associações resolvidas por id e custos calculados pela estratégia).
 */
@Mapper
public interface QuoteMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "usuarioNome", source = "usuario.nome")
    @Mapping(target = "impressoraId", source = "impressora.id")
    @Mapping(target = "impressoraNome", source = "impressora.nome")
    @Mapping(target = "filamentoId", source = "filamento.id")
    @Mapping(target = "filamentoNome", source = "filamento.nome")
    @Mapping(target = "pedidoId", source = "pedido.id")
    @Mapping(target = "pedidoNumero", source = "pedido.numero")
    QuoteResponse toResponse(Quote quote);

    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "preco", source = "precoEfetivo")
    PublicQuoteResponse toPublicResponse(Quote quote);
}
