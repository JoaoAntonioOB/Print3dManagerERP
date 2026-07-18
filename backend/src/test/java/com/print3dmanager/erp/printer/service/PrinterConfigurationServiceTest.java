package com.print3dmanager.erp.printer.service;

import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.printer.mapper.PrinterMapper;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import com.print3dmanager.erp.printer.repository.PrinterConfigurationRepository;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrinterConfigurationServiceTest {

    @Mock
    private PrinterConfigurationRepository configRepository;
    @Mock
    private PrinterRepository printerRepository;
    @Mock
    private PrinterMapper printerMapper;

    @InjectMocks
    private PrinterConfigurationService service;

    @Test
    @DisplayName("efetiva opcional: a configuração própria da impressora tem prioridade")
    void efetivaPrefereConfiguracaoPropria() {
        PrinterConfiguration propria = configuracao("0.80");
        when(configRepository.findByImpressoraId(1L)).thenReturn(Optional.of(propria));

        Optional<PrinterConfiguration> efetiva = service.buscarEfetivaOpcional(1L);

        assertThat(efetiva).containsSame(propria);
        verify(configRepository, never()).findByImpressoraIsNull();
    }

    @Test
    @DisplayName("efetiva opcional: sem configuração própria, cai na global")
    void efetivaCaiNaGlobal() {
        PrinterConfiguration global = configuracao("0.95");
        when(configRepository.findByImpressoraId(1L)).thenReturn(Optional.empty());
        when(configRepository.findByImpressoraIsNull()).thenReturn(Optional.of(global));

        assertThat(service.buscarEfetivaOpcional(1L)).containsSame(global);
    }

    @Test
    @DisplayName("efetiva opcional: sem nenhuma configuração, retorna vazio (sem exceção)")
    void efetivaSemNenhumaConfiguracao() {
        when(configRepository.findByImpressoraId(1L)).thenReturn(Optional.empty());
        when(configRepository.findByImpressoraIsNull()).thenReturn(Optional.empty());

        assertThat(service.buscarEfetivaOpcional(1L)).isEmpty();
    }

    @Test
    @DisplayName("global: sem configuração cadastrada, 404 orienta o PUT")
    void globalInexistenteOrientaCadastro() {
        when(configRepository.findByImpressoraIsNull()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarGlobal())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PUT /printers/config");
    }

    @Test
    @DisplayName("remover própria: impressora sem configuração própria → 404")
    void removerSemConfiguracaoPropria() {
        when(printerRepository.findById(1L)).thenReturn(Optional.of(new Printer()));
        when(configRepository.findByImpressoraId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removerDaImpressora(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(configRepository, never()).delete(any());
    }

    @Test
    @DisplayName("remover própria: configuração existente é excluída")
    void removerConfiguracaoPropria() {
        PrinterConfiguration propria = configuracao("0.80");
        when(printerRepository.findById(1L)).thenReturn(Optional.of(new Printer()));
        when(configRepository.findByImpressoraId(1L)).thenReturn(Optional.of(propria));

        service.removerDaImpressora(1L);

        verify(configRepository).delete(propria);
    }

    @Test
    @DisplayName("efetiva por impressora: impressora inexistente → 404")
    void efetivaImpressoraInexistente() {
        when(printerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarEfetiva(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PrinterConfiguration configuracao(String valorKwh) {
        PrinterConfiguration config = new PrinterConfiguration();
        config.setValorKwh(new BigDecimal(valorKwh));
        config.setValorHoraMaquina(new BigDecimal("8.00"));
        config.setCustoDesgasteHora(BigDecimal.ZERO);
        config.setMarkupPadrao(new BigDecimal("100"));
        return config;
    }
}
