-- =====================================================================
-- V9 — Transações financeiras
-- Receitas e despesas, com vínculo opcional a pedido e cliente
-- (base do módulo Financeiro e dos indicadores do Dashboard).
-- =====================================================================

CREATE TABLE transacoes_financeiras (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo            VARCHAR(10)     NOT NULL,
    categoria       VARCHAR(60)     NOT NULL,
    descricao       VARCHAR(255)    NOT NULL,
    valor           NUMERIC(12,2)   NOT NULL,
    data_transacao  DATE            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDENTE',
    forma_pagamento VARCHAR(30),
    pedido_id       BIGINT,
    cliente_id      BIGINT,
    observacoes     TEXT,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_transacoes_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    CONSTRAINT fk_transacoes_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_transacoes_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_transacoes_status CHECK (status IN ('PENDENTE', 'PAGA', 'CANCELADA')),
    CONSTRAINT ck_transacoes_valor CHECK (valor > 0)
);

CREATE INDEX idx_transacoes_data ON transacoes_financeiras (data_transacao);
CREATE INDEX idx_transacoes_tipo ON transacoes_financeiras (tipo);
CREATE INDEX idx_transacoes_status ON transacoes_financeiras (status);

COMMENT ON TABLE  transacoes_financeiras       IS 'Receitas e despesas do módulo Financeiro';
COMMENT ON COLUMN transacoes_financeiras.valor IS 'Sempre positivo — o sinal é dado pela coluna tipo';
