-- =====================================================================
-- V5 — Itens de estoque (insumos gerais)
-- Peças, parafusos, embalagens, componentes etc. O estoque de
-- filamento é controlado na própria tabela de filamentos.
-- =====================================================================

CREATE TABLE itens_estoque (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                VARCHAR(120)    NOT NULL,
    descricao           TEXT,
    categoria           VARCHAR(60),
    quantidade          NUMERIC(12,3)   NOT NULL DEFAULT 0,
    unidade_medida      VARCHAR(10)     NOT NULL DEFAULT 'UN',
    quantidade_minima   NUMERIC(12,3)   NOT NULL DEFAULT 0,
    custo_unitario      NUMERIC(12,2),
    localizacao         VARCHAR(120),
    ativo               BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ck_itens_estoque_quantidade CHECK (quantidade >= 0)
);

CREATE INDEX idx_itens_estoque_categoria ON itens_estoque (categoria);

COMMENT ON TABLE itens_estoque IS 'Insumos gerais do estoque (exceto filamentos)';
