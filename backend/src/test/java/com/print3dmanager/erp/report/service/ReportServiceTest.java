package com.print3dmanager.erp.report.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.report.dto.PdfReportFile;
import com.print3dmanager.erp.report.repository.ReportQueryRepository;
import com.print3dmanager.erp.financial.service.FinancialTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportQueryRepository reportQueryRepository;
    @Mock
    private FinancialTransactionService financialTransactionService;

    @InjectMocks
    private ReportService service;

    @Test
    @DisplayName("resolverPeriodo: período de até 366 dias é aceito normalmente")
    void periodoDentroDoLimiteEAceito() {
        when(reportQueryRepository.pedidosDoPeriodo(any(), any(), isNull()))
                .thenReturn(List.of());

        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = de.plusDays(365); // 366 dias inclusive

        PdfReportFile relatorio = service.pedidos(de, ate, null);

        assertThat(relatorio.conteudo()).isNotEmpty();
    }

    @Test
    @DisplayName("resolverPeriodo: período maior que 366 dias é rejeitado com BusinessException")
    void periodoAcimaDoLimiteEhRejeitado() {
        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = de.plusDays(366); // 367 dias inclusive — excede o teto

        assertThatThrownBy(() -> service.pedidos(de, ate, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("366");
    }

    @Test
    @DisplayName("resolverPeriodo: teto vale também para o relatório de consumo de filamento")
    void periodoAcimaDoLimiteEhRejeitadoNoConsumoDeFilamento() {
        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = de.plusDays(400);

        assertThatThrownBy(() -> service.consumoFilamento(de, ate))
                .isInstanceOf(BusinessException.class);
    }
}
