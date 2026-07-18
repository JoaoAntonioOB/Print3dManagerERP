package com.print3dmanager.erp.order.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.config.StorageProperties;
import com.print3dmanager.erp.order.dto.ModelFileDownload;
import com.print3dmanager.erp.order.dto.OrderItemResponse;
import com.print3dmanager.erp.order.mapper.OrderMapper;
import com.print3dmanager.erp.order.model.OrderItem;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.repository.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Map;
import java.util.UUID;

/**
 * Arquivos de modelo 3D (STL/3MF) dos itens de pedido, em disco no diretório
 * de uploads ({@code application.storage.upload-dir}, volume Docker em prod).
 *
 * <p>Regras: anexar/remover segue a regra de edição do pedido (somente
 * PENDENTE); download é permitido em qualquer status. O arquivo é gravado
 * como {@code modelos/<uuid>_<nome-sanitizado>} e o caminho relativo fica
 * em {@code itens_pedido.arquivo_modelo}; um novo upload substitui o
 * anterior (o arquivo antigo é apagado do disco).</p>
 */
@Service
@Slf4j
public class OrderItemFileService {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "stl", "model/stl",
            "3mf", "model/3mf");
    private static final String SUBDIRETORIO = "modelos";
    private static final int TAMANHO_MAXIMO_NOME = 100;

    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final Path raiz;

    public OrderItemFileService(OrderItemRepository orderItemRepository,
                                OrderMapper orderMapper,
                                StorageProperties storageProperties) {
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.raiz = Path.of(storageProperties.uploadDir()).toAbsolutePath().normalize();
    }

    /** Anexa (ou substitui) o arquivo de modelo do item. Pedido deve estar PENDENTE. */
    @Transactional
    public OrderItemResponse anexar(Long pedidoId, Long itemId, MultipartFile arquivo) {
        OrderItem item = obterItemDoPedido(pedidoId, itemId);
        exigirPedidoPendente(item);

        String nomeSanitizado = validarESanitizar(arquivo);
        String nomeArmazenado = "%s_%s".formatted(UUID.randomUUID(), nomeSanitizado);
        Path destino = raiz.resolve(SUBDIRETORIO).resolve(nomeArmazenado);
        try {
            Files.createDirectories(destino.getParent());
            arquivo.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Falha ao gravar o arquivo de modelo no armazenamento.", e);
        }

        removerArquivoFisico(item.getArquivoModelo());
        item.setArquivoModelo(SUBDIRETORIO + "/" + nomeArmazenado);
        return orderMapper.toItemResponse(item);
    }

    /** Conteúdo do arquivo do item para download (qualquer status do pedido). */
    @Transactional(readOnly = true)
    public ModelFileDownload baixar(Long pedidoId, Long itemId) {
        OrderItem item = obterItemDoPedido(pedidoId, itemId);
        if (item.getArquivoModelo() == null) {
            throw new ResourceNotFoundException(
                    "O item %d não possui arquivo de modelo anexado.".formatted(itemId));
        }
        Path caminho = resolverSeguro(item.getArquivoModelo());
        if (!Files.isRegularFile(caminho)) {
            throw new ResourceNotFoundException(
                    "O arquivo de modelo do item %d não foi encontrado no armazenamento."
                            .formatted(itemId));
        }
        String nomeDownload = nomeParaDownload(item.getArquivoModelo());
        return new ModelFileDownload(
                new FileSystemResource(caminho),
                nomeDownload,
                caminho.toFile().length(),
                CONTENT_TYPES.getOrDefault(extensaoDe(nomeDownload),
                        "application/octet-stream"));
    }

    /** Remove o arquivo do item (disco + coluna). Pedido deve estar PENDENTE. */
    @Transactional
    public void remover(Long pedidoId, Long itemId) {
        OrderItem item = obterItemDoPedido(pedidoId, itemId);
        exigirPedidoPendente(item);
        if (item.getArquivoModelo() == null) {
            throw new ResourceNotFoundException(
                    "O item %d não possui arquivo de modelo anexado.".formatted(itemId));
        }
        removerArquivoFisico(item.getArquivoModelo());
        item.setArquivoModelo(null);
    }

    /**
     * Apaga do disco o arquivo apontado pelo caminho relativo, se existir.
     * Usado também pelo {@link OrderService} ao excluir pedidos/itens.
     * Falha de I/O não interrompe a operação de negócio — apenas loga.
     */
    public void removerArquivoFisico(String arquivoModelo) {
        if (arquivoModelo == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolverSeguro(arquivoModelo));
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo de modelo {}: {}",
                    arquivoModelo, e.getMessage());
        }
    }

    /** Valida presença, nome e extensão; devolve o nome sanitizado (ASCII seguro). */
    private String validarESanitizar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("O arquivo de modelo enviado está vazio.");
        }
        String original = arquivo.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new BusinessException("O arquivo enviado não tem nome.");
        }
        String nome = sanitizar(original);
        String extensao = extensaoDe(nome);
        if (!CONTENT_TYPES.containsKey(extensao)) {
            throw new BusinessException(
                    "Extensão de arquivo não suportada: só são aceitos modelos .stl e .3mf.");
        }
        return nome;
    }

    /** Remove diretórios, acentos e caracteres fora de [a-zA-Z0-9._-]; limita o tamanho. */
    private String sanitizar(String original) {
        String nome = original.substring(
                Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        nome = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        nome = nome.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            String extensao = extensaoDe(nome);
            nome = nome.substring(0, TAMANHO_MAXIMO_NOME - extensao.length() - 1)
                    + "." + extensao;
        }
        return nome;
    }

    private String extensaoDe(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase();
    }

    /** Nome de download: o que vem depois do prefixo UUID do nome armazenado. */
    private String nomeParaDownload(String arquivoModelo) {
        String nome = arquivoModelo.substring(arquivoModelo.lastIndexOf('/') + 1);
        int separador = nome.indexOf('_');
        return separador < 0 ? nome : nome.substring(separador + 1);
    }

    /** Resolve o caminho relativo dentro da raiz de uploads (barra path traversal). */
    private Path resolverSeguro(String relativo) {
        Path caminho = raiz.resolve(relativo).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new BusinessException("Caminho de arquivo inválido.");
        }
        return caminho;
    }

    private void exigirPedidoPendente(OrderItem item) {
        OrderStatus status = item.getPedido().getStatus();
        if (status != OrderStatus.PENDENTE) {
            throw new BusinessException(
                    "Somente pedidos PENDENTES podem ter arquivos de modelo alterados. "
                            + "Status atual: %s.".formatted(status));
        }
    }

    private OrderItem obterItemDoPedido(Long pedidoId, Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de pedido", itemId));
        if (!item.getPedido().getId().equals(pedidoId)) {
            throw new ResourceNotFoundException(
                    "O item %d não pertence ao pedido %d.".formatted(itemId, pedidoId));
        }
        return item;
    }
}
