package com.print3dmanager.erp.order.service;

import com.print3dmanager.erp.common.exception.BusinessException;
import com.print3dmanager.erp.common.exception.ResourceNotFoundException;
import com.print3dmanager.erp.config.StorageProperties;
import com.print3dmanager.erp.order.dto.ModelFileDownload;
import com.print3dmanager.erp.order.mapper.OrderMapper;
import com.print3dmanager.erp.order.model.Order;
import com.print3dmanager.erp.order.model.OrderItem;
import com.print3dmanager.erp.order.model.OrderStatus;
import com.print3dmanager.erp.order.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderItemFileServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderMapper orderMapper;

    @TempDir
    private Path tempDir;

    private OrderItemFileService service;

    @BeforeEach
    void configurar() {
        service = new OrderItemFileService(orderItemRepository, orderMapper,
                new StorageProperties(tempDir.toString()));
    }

    @Test
    @DisplayName("anexar: grava em modelos/<uuid>_<nome sanitizado> e atualiza o item")
    void anexarSalvaArquivoComNomeSanitizado() throws Exception {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);

        service.anexar(5L, 3L, arquivo("Suporte Parede ç.stl", "solid peça"));

        assertThat(item.getArquivoModelo())
                .matches("modelos/[0-9a-f-]{36}_Suporte_Parede_c\\.stl");
        Path gravado = tempDir.resolve(item.getArquivoModelo());
        assertThat(gravado).exists();
        assertThat(Files.readString(gravado)).isEqualTo("solid peça");
    }

    @Test
    @DisplayName("anexar: novo upload substitui o arquivo anterior no disco")
    void anexarSubstituiArquivoAnterior() {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);

        service.anexar(5L, 3L, arquivo("v1.stl", "versão 1"));
        Path primeiro = tempDir.resolve(item.getArquivoModelo());

        service.anexar(5L, 3L, arquivo("v2.3mf", "versão 2"));

        assertThat(primeiro).doesNotExist();
        assertThat(item.getArquivoModelo()).endsWith(".3mf");
        assertThat(tempDir.resolve(item.getArquivoModelo())).exists();
    }

    @Test
    @DisplayName("anexar: extensão fora de .stl/.3mf é rejeitada")
    void anexarRejeitaExtensaoInvalida() {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);

        assertThatThrownBy(() -> service.anexar(5L, 3L, arquivo("peca.obj", "obj")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(".stl e .3mf");
        assertThat(item.getArquivoModelo()).isNull();
    }

    @Test
    @DisplayName("anexar: arquivo vazio é rejeitado")
    void anexarRejeitaArquivoVazio() {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);

        assertThatThrownBy(() -> service.anexar(5L, 3L, arquivo("peca.stl", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    @DisplayName("anexar: pedido fora de PENDENTE não pode receber arquivo")
    void anexarExigePedidoPendente() {
        OrderItem item = item(3L, 5L, OrderStatus.EM_PRODUCAO);
        stubItem(item);

        assertThatThrownBy(() -> service.anexar(5L, 3L, arquivo("peca.stl", "solid")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDENTES");
    }

    @Test
    @DisplayName("item que não pertence ao pedido informado → 404")
    void itemDeOutroPedido() {
        OrderItem item = item(3L, 99L, OrderStatus.PENDENTE);
        stubItem(item);

        assertThatThrownBy(() -> service.anexar(5L, 3L, arquivo("peca.stl", "solid")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    @DisplayName("baixar: devolve conteúdo, nome original sanitizado e content type do formato")
    void baixarDevolveConteudoENome() throws Exception {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);
        service.anexar(5L, 3L, arquivo("Suporte Parede.stl", "solid peça"));

        ModelFileDownload download = service.baixar(5L, 3L);

        assertThat(download.nomeArquivo()).isEqualTo("Suporte_Parede.stl");
        assertThat(download.contentType()).isEqualTo("model/stl");
        assertThat(download.recurso().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("solid peça");
        assertThat(download.tamanhoBytes()).isPositive();
    }

    @Test
    @DisplayName("baixar: item sem arquivo anexado → 404")
    void baixarSemArquivo() {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);

        assertThatThrownBy(() -> service.baixar(5L, 3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não possui arquivo");
    }

    @Test
    @DisplayName("remover: apaga do disco e limpa a coluna do item")
    void removerApagaDiscoEColuna() {
        OrderItem item = item(3L, 5L, OrderStatus.PENDENTE);
        stubItem(item);
        service.anexar(5L, 3L, arquivo("peca.stl", "solid"));
        Path gravado = tempDir.resolve(item.getArquivoModelo());

        service.remover(5L, 3L);

        assertThat(item.getArquivoModelo()).isNull();
        assertThat(gravado).doesNotExist();
    }

    @Test
    @DisplayName("remover arquivo físico: nulo e caminho inexistente não geram erro")
    void removerArquivoFisicoTolerante() {
        service.removerArquivoFisico(null);
        service.removerArquivoFisico("modelos/inexistente.stl");
    }

    // ===== fixtures =====

    private OrderItem item(long itemId, long pedidoId, OrderStatus status) {
        Order pedido = new Order();
        pedido.setId(pedidoId);
        pedido.setStatus(status);
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setPedido(pedido);
        return item;
    }

    private void stubItem(OrderItem item) {
        org.mockito.Mockito.when(orderItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));
    }

    private MockMultipartFile arquivo(String nome, String conteudo) {
        return new MockMultipartFile("arquivo", nome, "application/octet-stream",
                conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
