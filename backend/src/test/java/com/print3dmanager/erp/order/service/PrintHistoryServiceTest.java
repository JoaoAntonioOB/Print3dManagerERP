package com.print3dmanager.erp.order.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.order.dto.PrintCompleteRequest;
import com.print3dmanager.erp.order.dto.PrintFailRequest;
import com.print3dmanager.erp.order.dto.PrintStartRequest;
import com.print3dmanager.erp.order.mapper.PrintHistoryMapper;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderItem;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.model.PrintHistory;
import com.print3dmanager.erp.order.model.PrintStatus;
import com.print3dmanager.erp.order.repository.OrderItemRepository;
import com.print3dmanager.erp.order.repository.PrintHistoryRepository;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import com.print3dmanager.erp.printer.service.PrinterConfigurationService;
import com.print3dmanager.erp.user.model.User;
import com.print3dmanager.erp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintHistoryServiceTest {

    @Mock
    private PrintHistoryRepository printHistoryRepository;
    @Mock
    private PrinterRepository printerRepository;
    @Mock
    private FilamentRepository filamentRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PrinterConfigurationService printerConfigurationService;
    @Mock
    private PrintHistoryMapper printHistoryMapper;

    @InjectMocks
    private PrintHistoryService service;

    private static final Instant INICIO = Instant.parse("2026-07-01T10:00:00Z");

    // ===== início de job =====

    @Test
    @DisplayName("iniciar: impressora DISPONIVEL fica IMPRIMINDO e o job nasce EM_ANDAMENTO")
    void iniciarOcupaImpressora() {
        Printer impressora = impressora(PrinterStatus.DISPONIVEL, true);
        when(printerRepository.findById(1L)).thenReturn(Optional.of(impressora));
        when(userRepository.getReferenceById(9L)).thenReturn(usuario(9L));
        stubSalvarERecarregar();

        PrintStartRequest request =
                new PrintStartRequest(1L, null, null, INICIO, "Primeira peça");
        service.iniciar(request, 9L);

        assertThat(impressora.getStatus()).isEqualTo(PrinterStatus.IMPRIMINDO);
        verify(printHistoryRepository).save(any(PrintHistory.class));
    }

    @Test
    @DisplayName("iniciar: impressora ocupada não aceita segundo job simultâneo")
    void iniciarRejeitaImpressoraOcupada() {
        when(printerRepository.findById(1L))
                .thenReturn(Optional.of(impressora(PrinterStatus.IMPRIMINDO, true)));

        assertThatThrownBy(() -> service.iniciar(
                new PrintStartRequest(1L, null, null, null, null), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está disponível");
        verify(printHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("iniciar: impressora desativada é rejeitada")
    void iniciarRejeitaImpressoraDesativada() {
        when(printerRepository.findById(1L))
                .thenReturn(Optional.of(impressora(PrinterStatus.DISPONIVEL, false)));

        assertThatThrownBy(() -> service.iniciar(
                new PrintStartRequest(1L, null, null, null, null), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    @DisplayName("iniciar: item de pedido exige pedido EM_PRODUCAO")
    void iniciarExigePedidoEmProducao() {
        when(printerRepository.findById(1L))
                .thenReturn(Optional.of(impressora(PrinterStatus.DISPONIVEL, true)));
        OrderItem item = new OrderItem();
        Order pedido = new Order();
        pedido.setNumero("PED-2026-0001");
        pedido.setStatus(OrderStatus.PENDENTE);
        item.setPedido(pedido);
        when(orderItemRepository.findById(3L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.iniciar(
                new PrintStartRequest(1L, null, 3L, null, null), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está em produção");
    }

    // ===== finalização =====

    @Test
    @DisplayName("concluir: libera a impressora, soma horas, abate estoque e calcula o custo real")
    void concluirCalculaCustoEConsomeEstoque() {
        Filament filamento = filamento("90.00", "500.00");
        PrintHistory job = jobEmAndamento(filamento);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));
        when(printerConfigurationService.buscarEfetivaOpcional(1L))
                .thenReturn(Optional.of(configuracao("1.00", "10.00", "2.00")));

        // 120 min depois do início, 100 g, 0,8 kWh
        PrintCompleteRequest request = new PrintCompleteRequest(
                INICIO.plus(120, ChronoUnit.MINUTES),
                new BigDecimal("100"), new BigDecimal("0.8"), null);
        service.concluir(10L, request);

        assertThat(job.getStatus()).isEqualTo(PrintStatus.CONCLUIDA);
        assertThat(job.getTempoTotalMinutos()).isEqualTo(120);
        assertThat(job.getImpressora().getStatus()).isEqualTo(PrinterStatus.DISPONIVEL);
        assertThat(job.getImpressora().getHorasImpressaoTotal()).isEqualByComparingTo("2.00");
        assertThat(filamento.getQuantidadeEstoqueG()).isEqualByComparingTo("400.00");
        // filamento 100 g × 90/kg = 9,00 + energia 0,8 × 1,00 = 0,80
        // + máquina (10,00 + 2,00) × 2 h = 24,00 → 33,80
        assertThat(job.getCustoTotal()).isEqualByComparingTo("33.80");
    }

    @Test
    @DisplayName("concluir: sem filamento e sem configuração, o custo permanece nulo")
    void concluirSemDadosDeCusto() {
        PrintHistory job = jobEmAndamento(null);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));
        when(printerConfigurationService.buscarEfetivaOpcional(1L)).thenReturn(Optional.empty());

        service.concluir(10L, new PrintCompleteRequest(
                INICIO.plus(60, ChronoUnit.MINUTES), null, null, null));

        assertThat(job.getStatus()).isEqualTo(PrintStatus.CONCLUIDA);
        assertThat(job.getCustoTotal()).isNull();
    }

    @Test
    @DisplayName("falhar: material desperdiçado também sai do estoque e o motivo é registrado")
    void falharConsomeEstoqueERegistraMotivo() {
        Filament filamento = filamento("90.00", "500.00");
        PrintHistory job = jobEmAndamento(filamento);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));
        when(printerConfigurationService.buscarEfetivaOpcional(1L)).thenReturn(Optional.empty());

        PrintFailRequest request = new PrintFailRequest("Peça descolou da mesa",
                INICIO.plus(30, ChronoUnit.MINUTES), new BigDecimal("12.50"), null, null);
        service.falhar(10L, request);

        assertThat(job.getStatus()).isEqualTo(PrintStatus.FALHOU);
        assertThat(job.getMotivoFalha()).isEqualTo("Peça descolou da mesa");
        assertThat(filamento.getQuantidadeEstoqueG()).isEqualByComparingTo("487.50");
        assertThat(job.getImpressora().getStatus()).isEqualTo(PrinterStatus.DISPONIVEL);
    }

    @Test
    @DisplayName("finalizar: estoque insuficiente para o peso informado é rejeitado")
    void finalizarRejeitaEstoqueInsuficiente() {
        Filament filamento = filamento("90.00", "50.00");
        PrintHistory job = jobEmAndamento(filamento);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.concluir(10L, new PrintCompleteRequest(
                INICIO.plus(60, ChronoUnit.MINUTES), new BigDecimal("100"), null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("estoque");
        assertThat(filamento.getQuantidadeEstoqueG()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("finalizar: término anterior ao início é rejeitado")
    void finalizarRejeitaTerminoAntesDoInicio() {
        PrintHistory job = jobEmAndamento(null);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.concluir(10L, new PrintCompleteRequest(
                INICIO.minus(10, ChronoUnit.MINUTES), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anterior ao início");
    }

    @Test
    @DisplayName("finalizar: job já finalizado não pode ser finalizado de novo")
    void finalizarSomenteEmAndamento() {
        PrintHistory job = jobEmAndamento(null);
        job.setStatus(PrintStatus.CONCLUIDA);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.cancelar(10L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("EM_ANDAMENTO");
    }

    @Test
    @DisplayName("cancelar: sem corpo, apenas libera a impressora")
    void cancelarSemCorpoLiberaImpressora() {
        PrintHistory job = jobEmAndamento(null);
        when(printHistoryRepository.findDetalhadoById(10L)).thenReturn(Optional.of(job));
        when(printerConfigurationService.buscarEfetivaOpcional(1L)).thenReturn(Optional.empty());

        service.cancelar(10L, null);

        assertThat(job.getStatus()).isEqualTo(PrintStatus.CANCELADA);
        assertThat(job.getImpressora().getStatus()).isEqualTo(PrinterStatus.DISPONIVEL);
        assertThat(job.getCustoTotal()).isNull();
    }

    // ===== fixtures =====

    private Printer impressora(PrinterStatus status, boolean ativo) {
        Printer impressora = new Printer();
        impressora.setId(1L);
        impressora.setNome("Ender 3");
        impressora.setStatus(status);
        impressora.setAtivo(ativo);
        impressora.setHorasImpressaoTotal(BigDecimal.ZERO);
        return impressora;
    }

    private Filament filamento(String custoPorKg, String estoqueG) {
        Filament filamento = new Filament();
        filamento.setId(2L);
        filamento.setNome("PLA Azul");
        filamento.setAtivo(true);
        filamento.setCustoPorKg(new BigDecimal(custoPorKg));
        filamento.setQuantidadeEstoqueG(new BigDecimal(estoqueG));
        return filamento;
    }

    private PrinterConfiguration configuracao(String valorKwh, String valorHoraMaquina,
                                              String custoDesgasteHora) {
        PrinterConfiguration config = new PrinterConfiguration();
        config.setValorKwh(new BigDecimal(valorKwh));
        config.setValorHoraMaquina(new BigDecimal(valorHoraMaquina));
        config.setCustoDesgasteHora(new BigDecimal(custoDesgasteHora));
        return config;
    }

    private User usuario(Long id) {
        User usuario = new User();
        usuario.setId(id);
        return usuario;
    }

    private PrintHistory jobEmAndamento(Filament filamento) {
        PrintHistory job = new PrintHistory();
        job.setId(10L);
        job.setImpressora(impressora(PrinterStatus.IMPRIMINDO, true));
        job.setFilamento(filamento);
        job.setStatus(PrintStatus.EM_ANDAMENTO);
        job.setIniciadoEm(INICIO);
        return job;
    }

    /** save atribui o id e o recarregamento detalhado devolve o mesmo job. */
    private void stubSalvarERecarregar() {
        AtomicReference<PrintHistory> salvo = new AtomicReference<>();
        when(printHistoryRepository.save(any(PrintHistory.class))).thenAnswer(inv -> {
            PrintHistory job = inv.getArgument(0);
            job.setId(10L);
            salvo.set(job);
            return job;
        });
        when(printHistoryRepository.findDetalhadoById(10L))
                .thenAnswer(inv -> Optional.of(salvo.get()));
    }
}
