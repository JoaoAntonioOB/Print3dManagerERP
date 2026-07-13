package com.print3dmanager.erp.quote.service;

import com.print3dmanager.erp.client.model.Client;
import com.print3dmanager.erp.client.repository.ClientRepository;
import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.order.dto.OrderCreateRequest;
import com.print3dmanager.erp.order.dto.OrderItemRequest;
import com.print3dmanager.erp.order.dto.OrderResponse;
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
import com.print3dmanager.erp.quote.repository.QuoteSpecifications;
import com.print3dmanager.erp.quote.service.pricing.PricingInput;
import com.print3dmanager.erp.quote.service.pricing.PricingResult;
import com.print3dmanager.erp.quote.service.pricing.PricingStrategy;
import com.print3dmanager.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Regras de negócio de orçamentos: precificação delegada à estratégia
 * (custos decompostos + markup), número sequencial por ano, ciclo de vida
 * com link público de aprovação e conversão em pedido.
 */
@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final BigDecimal MARKUP_FALLBACK = new BigDecimal("100.00");
    private static final int TAMANHO_MAX_NOME_PECA = 160;

    /** Transições permitidas via PATCH; CONVERTIDO só pela conversão em pedido. */
    private static final Map<QuoteStatus, Set<QuoteStatus>> TRANSICOES = Map.of(
            QuoteStatus.RASCUNHO, Set.of(QuoteStatus.ENVIADO),
            QuoteStatus.ENVIADO, Set.of(QuoteStatus.RASCUNHO, QuoteStatus.APROVADO,
                    QuoteStatus.REJEITADO, QuoteStatus.EXPIRADO),
            QuoteStatus.APROVADO, Set.of(),
            QuoteStatus.REJEITADO, Set.of(),
            QuoteStatus.EXPIRADO, Set.of(),
            QuoteStatus.CONVERTIDO, Set.of());

    private final QuoteRepository quoteRepository;
    private final ClientRepository clientRepository;
    private final PrinterRepository printerRepository;
    private final FilamentRepository filamentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PrinterConfigurationService printerConfigurationService;
    private final PricingStrategy pricingStrategy;
    private final OrderService orderService;
    private final QuoteMapper quoteMapper;

    @Transactional(readOnly = true)
    public PageResponse<QuoteResponse> listar(String busca, QuoteStatus status, Long clienteId,
                                              Pageable pageable) {
        return PageResponse.de(
                quoteRepository.findAll(
                                QuoteSpecifications.comFiltros(busca, status, clienteId), pageable)
                        .map(quoteMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public QuoteResponse buscarPorId(Long id) {
        return quoteMapper.toResponse(obterOrcamentoDetalhado(id));
    }

    @Transactional
    public QuoteResponse criar(QuoteCreateRequest request, Long usuarioId) {
        Quote orcamento = new Quote();
        orcamento.setCliente(obterClienteAtivo(request.clienteId()));
        orcamento.setUsuario(userRepository.getReferenceById(usuarioId));
        orcamento.setImpressora(obterImpressoraAtiva(request.impressoraId()));
        orcamento.setFilamento(obterFilamentoAtivo(request.filamentoId()));
        orcamento.setDescricao(request.descricao());
        orcamento.setDataValidade(request.dataValidade());
        orcamento.setTempoImpressaoMinutos(request.tempoImpressaoMinutos());
        orcamento.setPesoEstimadoG(request.pesoEstimadoG());
        orcamento.setPrecoFinal(request.precoFinal());
        orcamento.setObservacoes(request.observacoes());
        orcamento.setMarkup(resolverMarkup(request.markup(), request.impressoraId()));
        orcamento.setNumero(gerarNumero());
        precificar(orcamento);

        quoteRepository.save(orcamento);
        return quoteMapper.toResponse(obterOrcamentoDetalhado(orcamento.getId()));
    }

    @Transactional
    public QuoteResponse atualizar(Long id, QuoteUpdateRequest request) {
        Quote orcamento = obterOrcamentoDetalhado(id);
        if (orcamento.getStatus() != QuoteStatus.RASCUNHO) {
            throw new BusinessException(
                    "Somente orçamentos em RASCUNHO podem ser editados. Status atual: %s. "
                            .formatted(orcamento.getStatus())
                            + "Volte um orçamento ENVIADO para RASCUNHO para editá-lo.");
        }

        orcamento.setCliente(obterClienteAtivo(request.clienteId()));
        orcamento.setImpressora(obterImpressoraAtiva(request.impressoraId()));
        orcamento.setFilamento(obterFilamentoAtivo(request.filamentoId()));
        orcamento.setDescricao(request.descricao());
        orcamento.setDataValidade(request.dataValidade());
        orcamento.setTempoImpressaoMinutos(request.tempoImpressaoMinutos());
        orcamento.setPesoEstimadoG(request.pesoEstimadoG());
        orcamento.setPrecoFinal(request.precoFinal());
        orcamento.setObservacoes(request.observacoes());
        orcamento.setMarkup(request.markup());
        precificar(orcamento);

        return quoteMapper.toResponse(orcamento);
    }

    @Transactional
    public QuoteResponse alterarStatus(Long id, QuoteStatusRequest request) {
        Quote orcamento = obterOrcamentoDetalhado(id);
        QuoteStatus novoStatus = request.status();
        if (novoStatus == QuoteStatus.CONVERTIDO) {
            throw new BusinessException(
                    "Use POST /quotes/{id}/converter para converter um orçamento em pedido.");
        }
        if (!TRANSICOES.get(orcamento.getStatus()).contains(novoStatus)) {
            throw new BusinessException(
                    "Transição de status inválida: %s → %s."
                            .formatted(orcamento.getStatus(), novoStatus));
        }
        orcamento.setStatus(novoStatus);
        if (novoStatus == QuoteStatus.APROVADO) {
            orcamento.setAprovadoEm(Instant.now());
        }
        return quoteMapper.toResponse(orcamento);
    }

    /** Exclusão física, permitida apenas em RASCUNHO. */
    @Transactional
    public void excluir(Long id) {
        Quote orcamento = obterOrcamento(id);
        if (orcamento.getStatus() != QuoteStatus.RASCUNHO) {
            throw new BusinessException(
                    "Somente orçamentos em RASCUNHO podem ser excluídos.");
        }
        quoteRepository.delete(orcamento);
    }

    /**
     * Converte um orçamento APROVADO em pedido: cria o pedido com um item
     * espelhando o orçamento (preço efetivo, peso, tempo e filamento) e
     * marca o orçamento como CONVERTIDO, vinculado ao pedido gerado.
     */
    @Transactional
    public OrderResponse converter(Long id, Long usuarioId) {
        Quote orcamento = obterOrcamentoDetalhado(id);
        if (orcamento.getStatus() != QuoteStatus.APROVADO) {
            throw new BusinessException(
                    "Somente orçamentos APROVADOS podem ser convertidos em pedido. "
                            + "Status atual: %s.".formatted(orcamento.getStatus()));
        }

        OrderItemRequest item = new OrderItemRequest(
                null,
                orcamento.getFilamento() == null ? null : orcamento.getFilamento().getId(),
                nomePecaDoOrcamento(orcamento),
                orcamento.getDescricao(),
                1,
                orcamento.getPesoEstimadoG(),
                orcamento.getTempoImpressaoMinutos(),
                orcamento.getPrecoEfetivo());
        OrderCreateRequest pedidoRequest = new OrderCreateRequest(
                orcamento.getCliente().getId(),
                null,
                BigDecimal.ZERO,
                "Gerado do orçamento %s.".formatted(orcamento.getNumero()),
                List.of(item));

        OrderResponse pedido = orderService.criar(pedidoRequest, usuarioId);
        orcamento.setPedido(orderRepository.getReferenceById(pedido.id()));
        orcamento.setStatus(QuoteStatus.CONVERTIDO);
        return pedido;
    }

    // ------------------------------------------------------------------
    // Link público (sem autenticação)
    // ------------------------------------------------------------------

    /** Visão pública pelo shareToken; RASCUNHO não é visível (404). */
    @Transactional
    public PublicQuoteResponse buscarPublico(String shareToken) {
        Quote orcamento = obterPorToken(shareToken);
        expirarSeVencido(orcamento);
        return quoteMapper.toPublicResponse(orcamento);
    }

    @Transactional
    public PublicQuoteResponse aprovarPublico(String shareToken) {
        Quote orcamento = obterPorToken(shareToken);
        expirarSeVencido(orcamento);
        exigirEnviado(orcamento, "aprovado");
        orcamento.setStatus(QuoteStatus.APROVADO);
        orcamento.setAprovadoEm(Instant.now());
        return quoteMapper.toPublicResponse(orcamento);
    }

    @Transactional
    public PublicQuoteResponse recusarPublico(String shareToken) {
        Quote orcamento = obterPorToken(shareToken);
        expirarSeVencido(orcamento);
        exigirEnviado(orcamento, "recusado");
        orcamento.setStatus(QuoteStatus.REJEITADO);
        return quoteMapper.toPublicResponse(orcamento);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    /** Executa a estratégia de precificação e grava os custos decompostos. */
    private void precificar(Quote orcamento) {
        Long impressoraId = orcamento.getImpressora() == null
                ? null : orcamento.getImpressora().getId();
        PricingResult resultado = pricingStrategy.calcular(new PricingInput(
                orcamento.getFilamento(),
                orcamento.getImpressora(),
                printerConfigurationService.buscarEfetivaOpcional(impressoraId),
                orcamento.getPesoEstimadoG(),
                orcamento.getTempoImpressaoMinutos(),
                orcamento.getMarkup()));

        orcamento.setCustoFilamento(resultado.custoFilamento());
        orcamento.setCustoEnergia(resultado.custoEnergia());
        orcamento.setCustoHoraMaquina(resultado.custoHoraMaquina());
        orcamento.setCustoDesgaste(resultado.custoDesgaste());
        orcamento.setPrecoSugerido(resultado.precoSugerido());
    }

    /** Markup informado > markup padrão da configuração efetiva > 100%. */
    private BigDecimal resolverMarkup(BigDecimal informado, Long impressoraId) {
        if (informado != null) {
            return informado;
        }
        return printerConfigurationService.buscarEfetivaOpcional(impressoraId)
                .map(PrinterConfiguration::getMarkupPadrao)
                .orElse(MARKUP_FALLBACK);
    }

    /** ENVIADO com validade vencida expira no acesso (expiração preguiçosa). */
    private void expirarSeVencido(Quote orcamento) {
        if (orcamento.getStatus() == QuoteStatus.ENVIADO
                && orcamento.getDataValidade() != null
                && orcamento.getDataValidade().isBefore(LocalDate.now())) {
            orcamento.setStatus(QuoteStatus.EXPIRADO);
        }
    }

    private void exigirEnviado(Quote orcamento, String acao) {
        if (orcamento.getStatus() == QuoteStatus.EXPIRADO) {
            throw new BusinessException(
                    "Este orçamento expirou em %s e não pode mais ser %s. "
                            .formatted(orcamento.getDataValidade(), acao)
                            + "Solicite um novo orçamento.");
        }
        if (orcamento.getStatus() != QuoteStatus.ENVIADO) {
            throw new BusinessException(
                    "Este orçamento não está mais aguardando resposta (status: %s)."
                            .formatted(orcamento.getStatus()));
        }
    }

    private String nomePecaDoOrcamento(Quote orcamento) {
        String base = orcamento.getDescricao() == null || orcamento.getDescricao().isBlank()
                ? "Orçamento %s".formatted(orcamento.getNumero())
                : orcamento.getDescricao().trim();
        return base.length() <= TAMANHO_MAX_NOME_PECA
                ? base : base.substring(0, TAMANHO_MAX_NOME_PECA);
    }

    /** ORC-2026-0001; mesma técnica de serialização usada nos pedidos. */
    private String gerarNumero() {
        int ano = Year.now().getValue();
        quoteRepository.travarGeracaoNumero(ano);
        String prefixo = "ORC-%d-".formatted(ano);
        int proximo = quoteRepository.findTopByNumeroStartingWithOrderByIdDesc(prefixo)
                .map(ultimo -> Integer.parseInt(
                        ultimo.getNumero().substring(prefixo.length())) + 1)
                .orElse(1);
        return "%s%04d".formatted(prefixo, proximo);
    }

    private Quote obterPorToken(String shareToken) {
        UUID token;
        try {
            token = UUID.fromString(shareToken);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Orçamento não encontrado para este link.");
        }
        Quote orcamento = quoteRepository.findByShareToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orçamento não encontrado para este link."));
        if (orcamento.getStatus() == QuoteStatus.RASCUNHO) {
            throw new ResourceNotFoundException("Orçamento não encontrado para este link.");
        }
        return orcamento;
    }

    private Client obterClienteAtivo(Long id) {
        Client cliente = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        if (!cliente.isAtivo()) {
            throw new BusinessException(
                    "O cliente %s está desativado e não pode receber novos orçamentos."
                            .formatted(cliente.getNome()));
        }
        return cliente;
    }

    private Printer obterImpressoraAtiva(Long id) {
        if (id == null) {
            return null;
        }
        Printer impressora = printerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impressora", id));
        if (!impressora.isAtivo()) {
            throw new BusinessException(
                    "A impressora %s está desativada.".formatted(impressora.getNome()));
        }
        return impressora;
    }

    private Filament obterFilamentoAtivo(Long id) {
        if (id == null) {
            return null;
        }
        Filament filamento = filamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filamento", id));
        if (!filamento.isAtivo()) {
            throw new BusinessException(
                    "O filamento %s está desativado.".formatted(filamento.getNome()));
        }
        return filamento;
    }

    private Quote obterOrcamento(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento", id));
    }

    private Quote obterOrcamentoDetalhado(Long id) {
        return quoteRepository.findDetalhadoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento", id));
    }
}
