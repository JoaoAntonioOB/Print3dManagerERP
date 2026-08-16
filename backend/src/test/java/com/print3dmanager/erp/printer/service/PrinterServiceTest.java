package com.print3dmanager.erp.printer.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.order.model.PrintHistory;
import com.print3dmanager.erp.order.model.PrintStatus;
import com.print3dmanager.erp.order.repository.PrintHistoryRepository;
import com.print3dmanager.erp.printer.dto.PrinterResponse;
import com.print3dmanager.erp.printer.mapper.PrinterMapper;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrinterServiceTest {

    @Mock
    private PrinterRepository printerRepository;
    @Mock
    private PrinterMapper printerMapper;
    @Mock
    private PrintHistoryRepository printHistoryRepository;

    @InjectMocks
    private PrinterService service;

    @Test
    @DisplayName("alterarStatus: bloqueia troca de status quando há job EM_ANDAMENTO na impressora")
    void alterarStatusBloqueiaComJobEmAndamento() {
        Printer impressora = impressora(PrinterStatus.IMPRIMINDO, true);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(impressora));
        PrintHistory job = new PrintHistory();
        job.setId(42L);
        when(printHistoryRepository.findFirstByImpressoraIdAndStatus(1L, PrintStatus.EM_ANDAMENTO))
                .thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.alterarStatus(1L, PrinterStatus.EM_MANUTENCAO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("job #42");
        assertThat(impressora.getStatus()).isEqualTo(PrinterStatus.IMPRIMINDO);
    }

    @Test
    @DisplayName("alterarStatus: sem job ativo, a troca de status é aplicada normalmente")
    void alterarStatusAplicaSemJobAtivo() {
        Printer impressora = impressora(PrinterStatus.DISPONIVEL, true);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(impressora));
        when(printHistoryRepository.findFirstByImpressoraIdAndStatus(1L, PrintStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(printerMapper.toResponse(impressora)).thenReturn(new PrinterResponse(
                1L, "Ender 3", null, null, PrinterStatus.EM_MANUTENCAO, null, null, null, null,
                null, null, null, null, true, null, null));

        service.alterarStatus(1L, PrinterStatus.EM_MANUTENCAO);

        assertThat(impressora.getStatus()).isEqualTo(PrinterStatus.EM_MANUTENCAO);
    }

    @Test
    @DisplayName("desativar: bloqueia soft delete quando há job EM_ANDAMENTO na impressora")
    void desativarBloqueiaComJobEmAndamento() {
        Printer impressora = impressora(PrinterStatus.IMPRIMINDO, true);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(impressora));
        PrintHistory job = new PrintHistory();
        job.setId(7L);
        when(printHistoryRepository.findFirstByImpressoraIdAndStatus(1L, PrintStatus.EM_ANDAMENTO))
                .thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.desativar(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("job #7");
        assertThat(impressora.isAtivo()).isTrue();
        assertThat(impressora.getStatus()).isEqualTo(PrinterStatus.IMPRIMINDO);
    }

    @Test
    @DisplayName("desativar: sem job ativo, aplica o soft delete normalmente")
    void desativarAplicaSemJobAtivo() {
        Printer impressora = impressora(PrinterStatus.DISPONIVEL, true);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(impressora));
        when(printHistoryRepository.findFirstByImpressoraIdAndStatus(1L, PrintStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());

        service.desativar(1L);

        assertThat(impressora.isAtivo()).isFalse();
        assertThat(impressora.getStatus()).isEqualTo(PrinterStatus.INATIVA);
        verify(printerRepository, never()).save(any());
    }

    private Printer impressora(PrinterStatus status, boolean ativo) {
        Printer impressora = new Printer();
        impressora.setId(1L);
        impressora.setNome("Ender 3");
        impressora.setStatus(status);
        impressora.setAtivo(ativo);
        return impressora;
    }
}
