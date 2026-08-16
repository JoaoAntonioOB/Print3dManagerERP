package com.print3dmanager.erp.quote.service;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.client.repository.ClientRepository;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.order.dto.OrderCreateRequest;
import com.print3dmanager.erp.order.dto.OrderResponse;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.repository.OrderRepository;
import com.print3dmanager.erp.order.service.OrderService;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterConfiguration;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import com.print3dmanager.erp.printer.service.PrinterConfigurationService;
import com.print3dmanager.erp.quote.dto.PublicQuoteResponse;
import com.print3dmanager.erp.quote.dto.QuoteCreateRequest;
import com.print3dmanager.erp.quote.dto.QuoteResponse;
import com.print3dmanager.erp.quote.dto.QuoteStatusRequest;
import com.print3dmanager.erp.quote.dto.QuoteUpdateRequest;
import com.print3dmanager.erp.quote.mapper.QuoteMapper;
import com.print3dmanager.erp.quote.model.Quote;
import com.print3dmanager.erp.quote.model.QuoteStatus;
import com.print3dmanager.erp.quote.repository.QuoteRepository;
import com.print3dmanager.erp.quote.service.pricing.PricingInput;
import com.print3dmanager.erp.quote.service.pricing.PricingResult;
import com.print3dmanager.erp.quote.service.pricing.PricingStrategy;
import com.print3dmanager.erp.user.model.User;
import com.print3dmanager.erp.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negócio de {@link QuoteService}: resolução de markup (informado
 * > configuração efetiva > fallback 100%), validações de cliente/impressora/
 * filamento ativos, edição restrita a RASCUNHO, máquina de estados (CONVERTIDO
 * só pela conversão), exclusão restrita a RASCUNHO, conversão em pedido
 * (lock antes do check-then-act) e o ciclo do link público (token inválido,
 * RASCUNHO invisível, expiração preguiçosa).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PrinterRepository printerRepository;
    @Mock
    private FilamentRepository filamentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PrinterConfigurationService printerConfigurationService;
    @Mock
    private PricingStrategy pricingStrategy;
    @Mock
    private OrderService orderService;
    @Mock
    private QuoteMapper quoteMapper;

    @InjectMocks
    private QuoteService service;

    /** Estratégia de precificação e mapeamento — usados em criar()/atualizar(). */
    private void stubPrecificacaoEMapeamento() {
        when(pricingStrategy.calcular(any(PricingInput.class))).thenReturn(
                new PricingResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO));
        when(quoteMapper.toResponse(any(Quote.class))).thenReturn(respostaQuote());
    }

    /**
     * Stubs completos do fluxo de criar(): precificação, usuário e a releitura
     * "detalhada" pós-save. NÃO usar em testes de atualizar()/alterarStatus()
     * que já stubam findDetalhadoById(id) explicitamente — o stub genérico
     * de findDetalhadoById(any()) aqui seria exercido durante o próprio
     * registro desses stubs específicos (o "when(...)" do Mockito chama o
     * método real para gravar o matcher) e explodiria com NPE porque
     * {@code capturado} ainda não foi setado por um save() anterior.
     */
    private void stubCriarComuns() {
        stubPrecificacaoEMapeamento();
        when(userRepository.getReferenceById(anyLong())).thenReturn(usuario());
        // save() é chamado antes de findDetalhadoById() no fluxo de criar() — captura
        // a entidade salva para que a releitura "detalhada" devolva o mesmo objeto.
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> {
            capturado = inv.getArgument(0);
            return capturado;
        });
        when(quoteRepository.findDetalhadoById(any())).thenAnswer(inv -> Optional.of(capturado));
    }

    // ===== criar: validações de referência =====

    @Test
    @DisplayName("criar: cliente desativado é rejeitado")
    void criarRejeitaClienteDesativado() {
        Client cliente = cliente(1L, false);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.criar(request(1L, null, null, null), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativado");
    }

    @Test
    @DisplayName("criar: cliente inexistente → 404")
    void criarComClienteInexistente() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request(1L, null, null, null), 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("criar: impressora desativada é rejeitada")
    void criarRejeitaImpressoraDesativada() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        when(printerRepository.findById(7L)).thenReturn(Optional.of(impressora(7L, false)));

        assertThatThrownBy(() -> service.criar(request(1L, 7L, null, null), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    @DisplayName("criar: filamento desativado é rejeitado")
    void criarRejeitaFilamentoDesativado() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        when(filamentRepository.findById(3L)).thenReturn(Optional.of(filamento(3L, false)));

        assertThatThrownBy(() -> service.criar(request(1L, null, 3L, null), 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativado");
    }

    // ===== criar: resolução de markup =====

    @Test
    @DisplayName("markup: informado no request tem prioridade sobre a configuração efetiva")
    void markupInformadoTemPrioridade() {
        stubCriarComuns();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        when(quoteRepository.findTopByNumeroStartingWithOrderByIdDesc(any()))
                .thenReturn(Optional.empty());

        QuoteResponse resposta = criarECapturar(
                request(1L, null, null, new BigDecimal("50.00")));

        // markup resolvido sem consultar a configuração (só a precificação em si
        // consulta buscarEfetivaOpcional, para os custos de energia/hora-máquina)
        assertThat(capturado.getMarkup()).isEqualByComparingTo("50.00");
        assertThat(resposta).isNotNull();
    }

    @Test
    @DisplayName("markup: omitido usa o markupPadrao da configuração efetiva")
    void markupOmitidoUsaConfiguracaoEfetiva() {
        stubCriarComuns();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        when(quoteRepository.findTopByNumeroStartingWithOrderByIdDesc(any()))
                .thenReturn(Optional.empty());
        PrinterConfiguration config = new PrinterConfiguration();
        config.setMarkupPadrao(new BigDecimal("150.00"));
        when(printerConfigurationService.buscarEfetivaOpcional(null))
                .thenReturn(Optional.of(config));

        criarECapturar(request(1L, null, null, null));

        assertThat(capturado.getMarkup()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("markup: omitido sem configuração cai no fallback de 100%")
    void markupOmitidoSemConfiguracaoUsaFallback() {
        stubCriarComuns();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        when(quoteRepository.findTopByNumeroStartingWithOrderByIdDesc(any()))
                .thenReturn(Optional.empty());
        when(printerConfigurationService.buscarEfetivaOpcional(null))
                .thenReturn(Optional.empty());

        criarECapturar(request(1L, null, null, null));

        assertThat(capturado.getMarkup()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("criar: primeiro orçamento do ano recebe ORC-<ano>-0001")
    void criarGeraNumeroInicial() {
        stubCriarComuns();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        when(quoteRepository.findTopByNumeroStartingWithOrderByIdDesc(any()))
                .thenReturn(Optional.empty());

        criarECapturar(request(1L, null, null, new BigDecimal("10.00")));

        int ano = Year.now().getValue();
        assertThat(capturado.getNumero()).isEqualTo("ORC-%d-0001".formatted(ano));
    }

    @Test
    @DisplayName("criar: número continua a sequência do último orçamento do ano")
    void criarContinuaSequencia() {
        stubCriarComuns();
        int ano = Year.now().getValue();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));
        Quote ultimo = new Quote();
        ultimo.setNumero("ORC-%d-0007".formatted(ano));
        when(quoteRepository.findTopByNumeroStartingWithOrderByIdDesc("ORC-%d-".formatted(ano)))
                .thenReturn(Optional.of(ultimo));

        criarECapturar(request(1L, null, null, new BigDecimal("10.00")));

        assertThat(capturado.getNumero()).isEqualTo("ORC-%d-0008".formatted(ano));
    }

    // ===== atualizar =====

    @Test
    @DisplayName("atualizar: orçamento fora de RASCUNHO é rejeitado")
    void atualizarRejeitaForaDeRascunho() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.atualizar(1L,
                updateRequest(1L, null, null, new BigDecimal("100.00"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RASCUNHO");
    }

    @Test
    @DisplayName("atualizar: orçamento em RASCUNHO recalcula custos e markup")
    void atualizarRascunhoRecalcula() {
        stubPrecificacaoEMapeamento();
        Quote orcamento = orcamento(1L, QuoteStatus.RASCUNHO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, true)));

        service.atualizar(1L, updateRequest(1L, null, null, new BigDecimal("80.00")));

        assertThat(orcamento.getMarkup()).isEqualByComparingTo("80.00");
        verify(pricingStrategy).calcular(any(PricingInput.class));
    }

    // ===== alterarStatus (máquina de estados) =====

    @Test
    @DisplayName("alterarStatus: RASCUNHO → ENVIADO é permitida")
    void rascunhoParaEnviado() {
        stubPrecificacaoEMapeamento();
        Quote orcamento = orcamento(1L, QuoteStatus.RASCUNHO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        service.alterarStatus(1L, new QuoteStatusRequest(QuoteStatus.ENVIADO));

        assertThat(orcamento.getStatus()).isEqualTo(QuoteStatus.ENVIADO);
    }

    @Test
    @DisplayName("alterarStatus: ENVIADO → APROVADO grava a data de aprovação")
    void enviadoParaAprovadoGravaData() {
        stubPrecificacaoEMapeamento();
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        service.alterarStatus(1L, new QuoteStatusRequest(QuoteStatus.APROVADO));

        assertThat(orcamento.getStatus()).isEqualTo(QuoteStatus.APROVADO);
        assertThat(orcamento.getAprovadoEm()).isNotNull();
    }

    @Test
    @DisplayName("alterarStatus: transição inválida (RASCUNHO → APROVADO) é rejeitada")
    void transicaoInvalidaRejeitada() {
        Quote orcamento = orcamento(1L, QuoteStatus.RASCUNHO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.alterarStatus(1L,
                new QuoteStatusRequest(QuoteStatus.APROVADO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    @DisplayName("alterarStatus: CONVERTIDO via PATCH é sempre rejeitado, mesmo a partir de ENVIADO")
    void convertidoViaPatchSempreRejeitado() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.alterarStatus(1L,
                new QuoteStatusRequest(QuoteStatus.CONVERTIDO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("/converter");
    }

    @Test
    @DisplayName("alterarStatus: estados terminais (APROVADO, REJEITADO, EXPIRADO) não aceitam nova transição")
    void estadosTerminaisNaoTransicionam() {
        for (QuoteStatus terminal : List.of(QuoteStatus.APROVADO, QuoteStatus.REJEITADO,
                QuoteStatus.EXPIRADO)) {
            Quote orcamento = orcamento(1L, terminal);
            when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

            assertThatThrownBy(() -> service.alterarStatus(1L,
                    new QuoteStatusRequest(QuoteStatus.ENVIADO)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ===== excluir =====

    @Test
    @DisplayName("excluir: orçamento em RASCUNHO é removido")
    void excluirRascunho() {
        Quote orcamento = orcamento(1L, QuoteStatus.RASCUNHO);
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(orcamento));

        service.excluir(1L);

        verify(quoteRepository).delete(orcamento);
    }

    @Test
    @DisplayName("excluir: orçamento fora de RASCUNHO é rejeitado")
    void excluirForaDeRascunhoRejeitado() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(BusinessException.class);
        verify(quoteRepository, never()).delete(any(Quote.class));
    }

    // ===== converter =====

    @Test
    @DisplayName("converter: orçamento não APROVADO é rejeitado")
    void converterExigeAprovado() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.converter(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APROVADOS");
        verify(orderService, never()).criar(any(), any());
    }

    @Test
    @DisplayName("converter: adquire o lock de conversão antes de checar o status (evita corrida)")
    void converterAdquireLockAntesDeChecar() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.converter(1L, 9L)).isInstanceOf(BusinessException.class);

        InOrder ordem = inOrder(quoteRepository);
        ordem.verify(quoteRepository).travarConversao(1L);
        ordem.verify(quoteRepository).findDetalhadoById(1L);
    }

    @Test
    @DisplayName("converter: orçamento APROVADO gera pedido com item espelhando o orçamento "
            + "e marca CONVERTIDO, vinculado ao pedido")
    void converterGeraPedidoEMarcaConvertido() {
        Client cliente = cliente(1L, true);
        Quote orcamento = orcamento(1L, QuoteStatus.APROVADO);
        orcamento.setCliente(cliente);
        orcamento.setNumero("ORC-2026-0001");
        orcamento.setDescricao("Suporte de parede");
        orcamento.setPesoEstimadoG(new BigDecimal("100.00"));
        orcamento.setTempoImpressaoMinutos(60);
        orcamento.setPrecoSugerido(new BigDecimal("80.00"));
        when(quoteRepository.findDetalhadoById(1L)).thenReturn(Optional.of(orcamento));

        OrderResponse pedidoResposta = new OrderResponse(42L, "PED-2026-0001", 1L, "Cliente",
                9L, "User", null, null, null, new BigDecimal("80.00"), BigDecimal.ZERO, null,
                List.of(), null, null);
        when(orderService.criar(any(OrderCreateRequest.class), anyLong()))
                .thenReturn(pedidoResposta);
        Order pedidoRef = new Order();
        pedidoRef.setId(42L);
        when(orderRepository.getReferenceById(42L)).thenReturn(pedidoRef);

        OrderResponse resultado = service.converter(1L, 9L);

        assertThat(resultado).isSameAs(pedidoResposta);
        assertThat(orcamento.getStatus()).isEqualTo(QuoteStatus.CONVERTIDO);
        assertThat(orcamento.getPedido()).isSameAs(pedidoRef);

        ArgumentCaptor<OrderCreateRequest> captor =
                ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(orderService).criar(captor.capture(), org.mockito.ArgumentMatchers.eq(9L));
        OrderCreateRequest pedidoRequest = captor.getValue();
        assertThat(pedidoRequest.clienteId()).isEqualTo(1L);
        assertThat(pedidoRequest.itens()).hasSize(1);
        assertThat(pedidoRequest.itens().get(0).nomePeca()).isEqualTo("Suporte de parede");
        assertThat(pedidoRequest.itens().get(0).precoUnitario()).isEqualByComparingTo("80.00");
        assertThat(pedidoRequest.itens().get(0).quantidade()).isEqualTo(1);
    }

    // ===== público =====

    @Test
    @DisplayName("público: token que não é UUID válido → 404")
    void tokenNaoUuidRetorna404() {
        assertThatThrownBy(() -> service.buscarPublico("nao-e-um-uuid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("público: RASCUNHO não é visível no link (404)")
    void rascunhoInvisivelNoLinkPublico() {
        Quote orcamento = orcamento(1L, QuoteStatus.RASCUNHO);
        java.util.UUID token = orcamento.getShareToken();
        when(quoteRepository.findByShareToken(token)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.buscarPublico(token.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("público: ENVIADO com validade vencida expira na consulta (expiração preguiçosa)")
    void enviadoVencidoExpiraNaConsulta() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        orcamento.setDataValidade(LocalDate.now().minusDays(1));
        java.util.UUID token = orcamento.getShareToken();
        when(quoteRepository.findByShareToken(token)).thenReturn(Optional.of(orcamento));
        when(quoteMapper.toPublicResponse(orcamento)).thenReturn(respostaPublica());

        service.buscarPublico(token.toString());

        assertThat(orcamento.getStatus()).isEqualTo(QuoteStatus.EXPIRADO);
    }

    @Test
    @DisplayName("público: aprovar um orçamento expirado dá mensagem específica de expiração")
    void aprovarExpiradoMensagemEspecifica() {
        Quote orcamento = orcamento(1L, QuoteStatus.EXPIRADO);
        orcamento.setDataValidade(LocalDate.now().minusDays(3));
        java.util.UUID token = orcamento.getShareToken();
        when(quoteRepository.findByShareToken(token)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.aprovarPublico(token.toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    @DisplayName("público: aprovar um orçamento que não está ENVIADO dá mensagem genérica")
    void aprovarNaoEnviadoMensagemGenerica() {
        // CONVERTIDO (e não RASCUNHO/EXPIRADO) para cair na mensagem genérica
        Quote orcamento = orcamento(1L, QuoteStatus.CONVERTIDO);
        java.util.UUID token = orcamento.getShareToken();
        when(quoteRepository.findByShareToken(token)).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.aprovarPublico(token.toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está mais aguardando");
    }

    @Test
    @DisplayName("público: recusar um orçamento ENVIADO muda para REJEITADO")
    void recusarEnviadoRejeita() {
        Quote orcamento = orcamento(1L, QuoteStatus.ENVIADO);
        java.util.UUID token = orcamento.getShareToken();
        when(quoteRepository.findByShareToken(token)).thenReturn(Optional.of(orcamento));
        when(quoteMapper.toPublicResponse(orcamento)).thenReturn(respostaPublica());

        service.recusarPublico(token.toString());

        assertThat(orcamento.getStatus()).isEqualTo(QuoteStatus.REJEITADO);
    }

    // ===== helpers =====

    private Quote capturado;

    private QuoteResponse criarECapturar(QuoteCreateRequest request) {
        QuoteResponse resposta = service.criar(request, 9L);
        ArgumentCaptor<Quote> captor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        capturado = captor.getValue();
        return resposta;
    }

    private QuoteCreateRequest request(Long clienteId, Long impressoraId, Long filamentoId,
                                       BigDecimal markup) {
        return new QuoteCreateRequest(clienteId, impressoraId, filamentoId, "Peça teste", null,
                60, new BigDecimal("50.00"), markup, null, null);
    }

    private QuoteUpdateRequest updateRequest(Long clienteId, Long impressoraId, Long filamentoId,
                                             BigDecimal markup) {
        return new QuoteUpdateRequest(clienteId, impressoraId, filamentoId, "Peça teste", null,
                60, new BigDecimal("50.00"), markup, null, null);
    }

    private Client cliente(Long id, boolean ativo) {
        Client cliente = new Client();
        cliente.setId(id);
        cliente.setNome("Cliente Teste");
        cliente.setAtivo(ativo);
        return cliente;
    }

    private Printer impressora(Long id, boolean ativo) {
        Printer impressora = new Printer();
        impressora.setId(id);
        impressora.setNome("Impressora Teste");
        impressora.setAtivo(ativo);
        return impressora;
    }

    private Filament filamento(Long id, boolean ativo) {
        Filament filamento = new Filament();
        filamento.setId(id);
        filamento.setNome("Filamento Teste");
        filamento.setAtivo(ativo);
        return filamento;
    }

    private User usuario() {
        User usuario = new User();
        usuario.setId(9L);
        usuario.setNome("Usuário Teste");
        return usuario;
    }

    private Quote orcamento(Long id, QuoteStatus status) {
        Quote orcamento = new Quote();
        orcamento.setId(id);
        orcamento.setStatus(status);
        orcamento.setNumero("ORC-2026-0001");
        orcamento.setMarkup(new BigDecimal("100.00"));
        orcamento.setCliente(cliente(1L, true));
        return orcamento;
    }

    private QuoteResponse respostaQuote() {
        return new QuoteResponse(1L, "ORC-2026-0001", 1L, "Cliente", 9L, "Usuário", null, null,
                null, null, null, null, QuoteStatus.RASCUNHO, java.util.UUID.randomUUID(),
                "Descrição", null, 60, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.00"),
                BigDecimal.ZERO, null, BigDecimal.ZERO, null, null, null, null);
    }

    private PublicQuoteResponse respostaPublica() {
        return new PublicQuoteResponse("ORC-2026-0001", "Cliente", "Descrição",
                QuoteStatus.ENVIADO, null, 60, new BigDecimal("50.00"),
                new BigDecimal("100.00"), null, null);
    }
}
