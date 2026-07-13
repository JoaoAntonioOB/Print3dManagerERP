-- =====================================================================
-- V6 — Pedidos e itens do pedido
-- Número gerado pela aplicação (ex.: PED-2026-0001). Itens são
-- removidos em cascata junto com o pedido.
-- =====================================================================

CREATE TABLE pedidos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero                  VARCHAR(20)     NOT NULL,
    cliente_id              BIGINT          NOT NULL,
    usuario_id              BIGINT          NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE',
    data_entrega_prevista   DATE,
    data_entrega_realizada  DATE,
    valor_total             NUMERIC(12,2)   NOT NULL DEFAULT 0,
    desconto                NUMERIC(12,2)   NOT NULL DEFAULT 0,
    observacoes             TEXT,
    criado_em               TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_pedidos_numero UNIQUE (numero),
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_pedidos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_pedidos_status CHECK (
        status IN ('PENDENTE', 'EM_PRODUCAO', 'CONCLUIDO', 'ENTREGUE', 'CANCELADO')
    ),
    CONSTRAINT ck_pedidos_valores CHECK (valor_total >= 0 AND desconto >= 0)
);

CREATE INDEX idx_pedidos_cliente ON pedidos (cliente_id);
CREATE INDEX idx_pedidos_status ON pedidos (status);
CREATE INDEX idx_pedidos_criado_em ON pedidos (criado_em);

CREATE TABLE itens_pedido (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id               BIGINT          NOT NULL,
    filamento_id            BIGINT,
    nome_peca               VARCHAR(160)    NOT NULL,
    descricao               TEXT,
    quantidade              INTEGER         NOT NULL DEFAULT 1,
    peso_estimado_g         NUMERIC(10,2),
    tempo_impressao_minutos INTEGER,
    preco_unitario          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    arquivo_modelo          VARCHAR(255),
    criado_em               TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_itens_pedido_pedido FOREIGN KEY (pedido_id)
        REFERENCES pedidos (id) ON DELETE CASCADE,
    CONSTRAINT fk_itens_pedido_filamento FOREIGN KEY (filamento_id)
        REFERENCES filamentos (id),
    CONSTRAINT ck_itens_pedido_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_itens_pedido_preco CHECK (preco_unitario >= 0)
);

CREATE INDEX idx_itens_pedido_pedido ON itens_pedido (pedido_id);

COMMENT ON COLUMN itens_pedido.arquivo_modelo IS 'Caminho do arquivo STL/3MF no diretório de uploads';
