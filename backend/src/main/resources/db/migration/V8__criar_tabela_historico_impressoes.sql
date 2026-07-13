-- =====================================================================
-- V8 — Histórico de impressões
-- Registro de cada job de impressão: máquina, material, operador,
-- tempos, consumo e resultado (sucesso/falha).
-- =====================================================================

CREATE TABLE historico_impressoes (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    impressora_id       BIGINT          NOT NULL,
    filamento_id        BIGINT,
    item_pedido_id      BIGINT,
    usuario_id          BIGINT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'EM_ANDAMENTO',
    iniciado_em         TIMESTAMPTZ     NOT NULL,
    finalizado_em       TIMESTAMPTZ,
    tempo_total_minutos INTEGER,
    peso_utilizado_g    NUMERIC(10,2),
    consumo_energia_kwh NUMERIC(10,3),
    custo_total         NUMERIC(12,2),
    motivo_falha        TEXT,
    observacoes         TEXT,
    criado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_historico_impressora FOREIGN KEY (impressora_id) REFERENCES impressoras (id),
    CONSTRAINT fk_historico_filamento FOREIGN KEY (filamento_id) REFERENCES filamentos (id),
    CONSTRAINT fk_historico_item_pedido FOREIGN KEY (item_pedido_id)
        REFERENCES itens_pedido (id) ON DELETE SET NULL,
    CONSTRAINT fk_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_historico_status CHECK (
        status IN ('EM_ANDAMENTO', 'CONCLUIDA', 'FALHOU', 'CANCELADA')
    ),
    CONSTRAINT ck_historico_periodo CHECK (
        finalizado_em IS NULL OR finalizado_em >= iniciado_em
    )
);

CREATE INDEX idx_historico_impressora ON historico_impressoes (impressora_id);
CREATE INDEX idx_historico_status ON historico_impressoes (status);
CREATE INDEX idx_historico_iniciado_em ON historico_impressoes (iniciado_em);

COMMENT ON TABLE historico_impressoes IS 'Registro de cada job de impressão executado';
