package com.print3dmanager.erp.dashboard.controller;

import com.print3dmanager.erp.dashboard.dto.DashboardResumoResponse;
import com.print3dmanager.erp.dashboard.dto.MonthlyFilamentUsagePoint;
import com.print3dmanager.erp.dashboard.dto.MonthlySalesPoint;
import com.print3dmanager.erp.dashboard.dto.PrintSuccessRateResponse;
import com.print3dmanager.erp.dashboard.dto.TopClientPoint;
import com.print3dmanager.erp.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Painel de indicadores: somente leitura, aberto a todos os perfis
 * internos (parâmetros fora da faixa são ajustados, não rejeitados).
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores agregados e séries para gráficos")
public class DashboardController {

    private static final String PODE_CONSULTAR =
            "hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR')";

    private final DashboardService dashboardService;

    @GetMapping("/resumo")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Indicadores gerais: contadores por status e destaques do mês")
    public DashboardResumoResponse resumo() {
        return dashboardService.resumo();
    }

    @GetMapping("/vendas-mensais")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Série mensal de pedidos abertos e faturamento entregue",
            description = "Sempre devolve os N meses completos (faltantes zerados), "
                    + "em ordem cronológica.")
    public List<MonthlySalesPoint> vendasMensais(
            @Parameter(description = "Quantidade de meses (1–60, incluindo o corrente)")
            @RequestParam(defaultValue = "12") int meses) {
        return dashboardService.vendasMensais(meses);
    }

    @GetMapping("/consumo-filamento")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Série mensal de gramas de filamento consumidas nas impressões")
    public List<MonthlyFilamentUsagePoint> consumoFilamento(
            @Parameter(description = "Quantidade de meses (1–60, incluindo o corrente)")
            @RequestParam(defaultValue = "12") int meses) {
        return dashboardService.consumoFilamentoMensal(meses);
    }

    @GetMapping("/taxa-sucesso")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Taxa de sucesso das impressões (concluídas × falhas)")
    public PrintSuccessRateResponse taxaSucesso() {
        return dashboardService.taxaSucessoImpressoes();
    }

    @GetMapping("/top-clientes")
    @PreAuthorize(PODE_CONSULTAR)
    @Operation(summary = "Maiores clientes por valor total de pedidos (cancelados fora)")
    public List<TopClientPoint> topClientes(
            @Parameter(description = "Quantidade de clientes (1–50)")
            @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.topClientes(limite);
    }
}
