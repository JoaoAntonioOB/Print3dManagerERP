package com.print3dmanager.erp.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Cliente ordenado por valor total de pedidos (cancelados fora)")
public record TopClientPoint(
        Long clienteId,
        String clienteNome,
        long pedidos,
        BigDecimal valorTotal
) {
}
