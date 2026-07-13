-- =====================================================================
-- V7 — Orçamentos
-- Cálculo: custo filamento + energia + hora máquina + desgaste
-- + margem de lucro (markup) = preço sugerido.
-- share_token: link público para o cliente aprovar sem login.
-- =====================================================================

CREATE TABLE orcamentos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero                  VARCHAR(20)     NOT NULL,
    cliente_id              BIGINT          NOT NULL,
    usuario_id              BIGINT          NOT NULL,
    impressora_id           BIGINT,
    filamento_id            BIGINT,
    pedido_id               BIGINT,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'RASCUNHO',
    share_token             UUID            NOT NULL DEFAULT gen_random_uuid(),
    descricao               TEXT,
    data_validade           DATE,
    tempo_impressao_minutos INTEGER,
    peso_estimado_g         NUMERIC(10,2),
    custo_filamento         NUMERIC(12,2)   NOT NULL DEFAULT 0,
    custo_energia           NUMERIC(12,2)   NOT NULL DEFAULT 0,
    custo_hora_maquina      NUMERIC(12,2)   NOT NULL DEFAULT 0,
    custo_desgaste          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    markup                  NUMERIC(5,2)    NOT NULL DEFAULT 100.00,
    preco_sugerido          NUMERIC(12,2)   NOT NULL DEFAULT 0,
    preco_final             NUMERIC(12,2),
    aprovado_em             TIMESTAMPTZ,
    observacoes             TEXT,
    criado_em               TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_orcamentos_numero UNIQUE (numero),
    CONSTRAINT uk_orcamentos_share_token UNIQUE (share_token),
    CONSTRAINT fk_orcamentos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_orcamentos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_orcamentos_impressora FOREIGN KEY (impressora_id) REFERENCES impressoras (id),
    CONSTRAINT fk_orcamentos_filamento FOREIGN KEY (filamento_id) REFERENCES filamentos (id),
    CONSTRAINT fk_orcamentos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    CONSTRAINT ck_orcamentos_status CHECK (
        status IN ('RASCUNHO', 'ENVIADO', 'APROVADO', 'REJEITADO', 'EXPIRADO', 'CONVERTIDO')
    ),
    CONSTRAINT ck_orcamentos_markup CHECK (markup >= 0)
);

CREATE INDEX idx_orcamentos_cliente ON orcamentos (cliente_id);
CREATE INDEX idx_orcamentos_status ON orcamentos (status);

COMMENT ON COLUMN orcamentos.share_token IS 'Token do link público de aprovação pelo cliente';
COMMENT ON COLUMN orcamentos.pedido_id   IS 'Pedido gerado quando o orçamento é convertido';
COMMENT ON COLUMN orcamentos.markup      IS 'Margem de lucro em porcentagem, editável pelo usuário';
