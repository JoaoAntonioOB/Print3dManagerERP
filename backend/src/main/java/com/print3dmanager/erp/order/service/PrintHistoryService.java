package com.print3dmanager.erp.order.service;

import com.print3dmanager.erp.common.dto.PageResponse;
import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.filament.model.Filament;
import com.print3dmanager.erp.filament.repository.FilamentRepository;
import com.print3dmanager.erp.order.dto.PrintCompleteRequest;
import com.print3dmanager.erp.order.dto.PrintFailRequest;
import com.print3dmanager.erp.order.dto.PrintHistoryResponse;
import com.print3dmanager.erp.order.dto.PrintStartRequest;
import com.print3dmanager.erp.order.mapper.PrintHistoryMapper;
import com.print3dmanager.erp.order.model.OrderItem;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.model.PrintHistory;
import com.print3dmanager.erp.order.model.PrintStatus;
import com.print3dmanager.erp.order.repository.OrderItemRepository;
import com.print3dmanager.erp.order.repository.PrintHistoryRepository;
import com.print3dmanager.erp.order.repository.PrintHistorySpecifications;
import com.print3dmanager.erp.printer.model.Printer;
import com.print3dmanager.erp.printer.model.PrinterStatus;
import com.print3dmanager.erp.printer.repository.PrinterRepository;
import com.print3dmanager.erp.printer.service.PrinterConfigurationService;
import com.print3dmanager.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Ciclo de vida dos jobs de impressão: iniciar ocupa a impressora
 * (IMPRIMINDO); concluir/falhar/cancelar libera a máquina, soma horas de
 * uso, consome o estoque do filamento e calcula o custo real do job com a
 * configuração efetiva da impressora (quando cadastrada).
 */
@Service
@RequiredArgsConstructor
public class PrintHistoryService {

    private static final BigDecimal GRAMAS_POR_KG = new BigDecimal("1000");
    private static final BigDecimal MINUTOS_POR_HORA = new BigDecimal("60");

    private final PrintHistoryRepository printHistoryRepository;
    private final PrinterRepository printerRepository;
    private final FilamentRepository filamentRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final PrinterConfigurationService printerConfigurationService;
    private final PrintHistoryMapper printHistoryMapper;

    @Transactional(readOnly = true)
    public PageResponse<PrintHistoryResponse> listar(Long impressoraId, PrintStatus status,
                                                     Long itemPedidoId, Instant de, Instant ate,
                                                     Pageable pageable) {
        return PageResponse.de(
                printHistoryRepository.findAll(
                                PrintHistorySpecifications.comFiltros(impressoraId, status,
                                        itemPedidoId, de, ate), pageable)
                        .map(printHistoryMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PrintHistoryResponse buscarPorId(Long id) {
        return printHistoryMapper.toResponse(obterJobDetalhado(id));
    }

    @Transactional
    public PrintHistoryResponse iniciar(PrintStartRequest request, Long usuarioId) {
        Printer impressora = obterImpressora(request.impressoraId());
        if (!impressora.isAtivo()) {
            throw new BusinessException(
                    "A impressora %s está desativada.".formatted(impressora.getNome()));
        }
        if (impressora.getStatus() != PrinterStatus.DISPONIVEL) {
            throw new BusinessException(
                    "A impressora %s não está disponível (status atual: %s)."
                            .formatted(impressora.getNome(), impressora.getStatus()));
        }

        PrintHistory job = new PrintHistory();
        job.setImpressora(impressora);
        job.setFilamento(obterFilamentoAtivo(request.filamentoId()));
        job.setItemPedido(obterItemEmProducao(request.itemPedidoId()));
        job.setUsuario(userRepository.getReferenceById(usuarioId));
        job.setIniciadoEm(request.iniciadoEm() == null ? Instant.now() : request.iniciadoEm());
        job.setObservacoes(request.observacoes());

        impressora.setStatus(PrinterStatus.IMPRIMINDO);
        printHistoryRepository.save(job);
        return printHistoryMapper.toResponse(obterJobDetalhado(job.getId()));
    }

    @Transactional
    public PrintHistoryResponse concluir(Long id, PrintCompleteRequest request) {
        PrintHistory job = obterJobDetalhado(id);
        finalizar(job, PrintStatus.CONCLUIDA, request.finalizadoEm(),
                request.pesoUtilizadoG(), request.consumoEnergiaKwh(), request.observacoes());
        return printHistoryMapper.toResponse(job);
    }

    @Transactional
    public PrintHistoryResponse falhar(Long id, PrintFailRequest request) {
        PrintHistory job = obterJobDetalhado(id);
        finalizar(job, PrintStatus.FALHOU, request.finalizadoEm(),
                request.pesoUtilizadoG(), request.consumoEnergiaKwh(), request.observacoes());
        job.setMotivoFalha(request.motivoFalha());
        return printHistoryMapper.toResponse(job);
    }

    @Transactional
    public PrintHistoryResponse cancelar(Long id, PrintCompleteRequest request) {
        PrintHistory job = obterJobDetalhado(id);
        finalizar(job, PrintStatus.CANCELADA,
                request == null ? null : request.finalizadoEm(),
                request == null ? null : request.pesoUtilizadoG(),
                request == null ? null : request.consumoEnergiaKwh(),
                request == null ? null : request.observacoes());
        return printHistoryMapper.toResponse(job);
    }

    /**
     * Encerramento comum: valida o período, libera a impressora e soma suas
     * horas de uso, abate o material gasto do estoque (também em falhas —
     * material desperdiçado saiu da bobina do mesmo jeito) e registra o custo.
     */
    private void finalizar(PrintHistory job, PrintStatus novoStatus, Instant finalizadoEm,
                           BigDecimal pesoUtilizadoG, BigDecimal consumoEnergiaKwh,
                           String observacoes) {
        if (job.getStatus() != PrintStatus.EM_ANDAMENTO) {
            throw new BusinessException(
                    "Somente jobs EM_ANDAMENTO podem ser finalizados. Status atual: %s."
                            .formatted(job.getStatus()));
        }

        Instant fim = finalizadoEm == null ? Instant.now() : finalizadoEm;
        if (fim.isBefore(job.getIniciadoEm())) {
            throw new BusinessException("O término não pode ser anterior ao início do job.");
        }

        job.setStatus(novoStatus);
        job.setFinalizadoEm(fim);
        job.setTempoTotalMinutos((int) Duration.between(job.getIniciadoEm(), fim).toMinutes());
        job.setPesoUtilizadoG(pesoUtilizadoG);
        job.setConsumoEnergiaKwh(consumoEnergiaKwh);
        if (observacoes != null) {
            job.setObservacoes(observacoes);
        }

        liberarImpressora(job);
        consumirFilamento(job);
        job.setCustoTotal(calcularCusto(job));
    }

    /** Devolve a impressora para DISPONIVEL e acumula as horas do job. */
    private void liberarImpressora(PrintHistory job) {
        Printer impressora = job.getImpressora();
        if (impressora.getStatus() == PrinterStatus.IMPRIMINDO) {
            impressora.setStatus(PrinterStatus.DISPONIVEL);
        }
        if (job.getTempoTotalMinutos() != null && job.getTempoTotalMinutos() > 0) {
            BigDecimal horas = BigDecimal.valueOf(job.getTempoTotalMinutos())
                    .divide(MINUTOS_POR_HORA, 2, RoundingMode.HALF_UP);
            BigDecimal atual = impressora.getHorasImpressaoTotal() == null
                    ? BigDecimal.ZERO : impressora.getHorasImpressaoTotal();
            impressora.setHorasImpressaoTotal(atual.add(horas));
        }
    }

    private void consumirFilamento(PrintHistory job) {
        Filament filamento = job.getFilamento();
        BigDecimal peso = job.getPesoUtilizadoG();
        if (filamento == null || peso == null) {
            return;
        }
        BigDecimal novoSaldo = filamento.getQuantidadeEstoqueG().subtract(peso);
        if (novoSaldo.signum() < 0) {
            throw new BusinessException(
                    ("O estoque do filamento %s (%s g) é menor que o peso informado (%s g). "
                            + "Ajuste o estoque em PATCH /filaments/{id}/estoque antes de "
                            + "finalizar o job.")
                            .formatted(filamento.getNome(),
                                    filamento.getQuantidadeEstoqueG().stripTrailingZeros()
                                            .toPlainString(),
                                    peso.stripTrailingZeros().toPlainString()));
        }
        filamento.setQuantidadeEstoqueG(novoSaldo);
    }

    /**
     * Custo real do job: filamento (peso × custo/kg) + energia (kWh × tarifa)
     * + máquina ((hora máquina + desgaste) × horas). Energia e máquina
     * dependem da configuração efetiva da impressora; componentes sem dados
     * são ignorados. Sem nenhum componente calculável, permanece null.
     */
    private BigDecimal calcularCusto(PrintHistory job) {
        BigDecimal custo = BigDecimal.ZERO;
        boolean calculavel = false;

        if (job.getFilamento() != null && job.getPesoUtilizadoG() != null) {
            custo = custo.add(job.getPesoUtilizadoG()
                    .multiply(job.getFilamento().getCustoPorKg())
                    .divide(GRAMAS_POR_KG, 4, RoundingMode.HALF_UP));
            calculavel = true;
        }

        var config = printerConfigurationService
                .buscarEfetivaOpcional(job.getImpressora().getId());
        if (config.isPresent()) {
            if (job.getConsumoEnergiaKwh() != null) {
                custo = custo.add(job.getConsumoEnergiaKwh()
                        .multiply(config.get().getValorKwh()));
                calculavel = true;
            }
            if (job.getTempoTotalMinutos() != null && job.getTempoTotalMinutos() > 0) {
                BigDecimal horas = BigDecimal.valueOf(job.getTempoTotalMinutos())
                        .divide(MINUTOS_POR_HORA, 4, RoundingMode.HALF_UP);
                custo = custo.add(config.get().getValorHoraMaquina()
                        .add(config.get().getCustoDesgasteHora())
                        .multiply(horas));
                calculavel = true;
            }
        }

        return calculavel ? custo.setScale(2, RoundingMode.HALF_UP) : null;
    }

    private Printer obterImpressora(Long id) {
        return printerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impressora", id));
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

    /** Item de pedido só pode ser impresso com o pedido EM_PRODUCAO. */
    private OrderItem obterItemEmProducao(Long id) {
        if (id == null) {
            return null;
        }
        OrderItem item = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de pedido", id));
        if (item.getPedido().getStatus() != OrderStatus.EM_PRODUCAO) {
            throw new BusinessException(
                    "O pedido %s não está em produção (status atual: %s)."
                            .formatted(item.getPedido().getNumero(),
                                    item.getPedido().getStatus()));
        }
        return item;
    }

    private PrintHistory obterJobDetalhado(Long id) {
        return printHistoryRepository.findDetalhadoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Impressão", id));
    }
}
