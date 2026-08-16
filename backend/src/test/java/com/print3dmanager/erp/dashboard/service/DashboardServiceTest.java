package com.print3dmanager.erp.dashboard.service;

import com.print3dmanager.erp.dashboard.dto.TopClientPoint;
import com.print3dmanager.erp.dashboard.repository.DashboardQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardQueryRepository dashboardQueryRepository;

    @InjectMocks
    private DashboardService service;

    @Captor
    private ArgumentCaptor<Instant> inicioCaptor;

    @Test
    @DisplayName("topClientes: filtra pela janela de meses informada (default 12), não pelo histórico completo")
    void topClientesUsaJanelaDeMeses() {
        when(dashboardQueryRepository.topClientes(inicioCaptor.capture(), eq(5)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, "Cliente A", 3L, new BigDecimal("150.00")}));

        List<TopClientPoint> resultado = service.topClientes(12, 5);

        Instant inicioEsperado = YearMonth.now(ZoneOffset.UTC).minusMonths(11)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        assertThat(inicioCaptor.getValue()).isEqualTo(inicioEsperado);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).clienteId()).isEqualTo(1L);
        assertThat(resultado.get(0).clienteNome()).isEqualTo("Cliente A");
        assertThat(resultado.get(0).pedidos()).isEqualTo(3L);
        assertThat(resultado.get(0).valorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("topClientes: meses fora da faixa (1–60) é ajustado, não rejeitado")
    void topClientesAjustaMesesForaDaFaixa() {
        when(dashboardQueryRepository.topClientes(inicioCaptor.capture(), eq(5)))
                .thenReturn(List.<Object[]>of());

        service.topClientes(1000, 5);

        Instant inicioEsperado = YearMonth.now(ZoneOffset.UTC).minusMonths(59)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        assertThat(inicioCaptor.getValue()).isEqualTo(inicioEsperado);
    }

    @Test
    @DisplayName("topClientes: limite fora da faixa (1–50) é ajustado, não rejeitado")
    void topClientesAjustaLimiteForaDaFaixa() {
        when(dashboardQueryRepository.topClientes(inicioCaptor.capture(), anyInt()))
                .thenReturn(List.<Object[]>of());

        service.topClientes(12, 500);

        org.mockito.Mockito.verify(dashboardQueryRepository).topClientes(inicioCaptor.getValue(), 50);
    }
}
