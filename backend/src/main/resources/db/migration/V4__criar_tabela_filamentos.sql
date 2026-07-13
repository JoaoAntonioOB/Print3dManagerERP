-- =====================================================================
-- V4 — Filamentos
-- Controle de material com estoque em gramas e custo por kg
-- (base do cálculo de custo de filamento nos orçamentos).
-- =====================================================================

CREATE TABLE filamentos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                    VARCHAR(120)    NOT NULL,
    marca                   VARCHAR(80),
    material                VARCHAR(20)     NOT NULL,
    cor                     VARCHAR(40),
    diametro_mm             NUMERIC(4,2)    NOT NULL DEFAULT 1.75,
    peso_bobina_g           NUMERIC(8,2),
    custo_por_kg            NUMERIC(10,2)   NOT NULL,
    quantidade_estoque_g    NUMERIC(12,2)   NOT NULL DEFAULT 0,
    estoque_minimo_g        NUMERIC(12,2)   NOT NULL DEFAULT 0,
    temperatura_bico        INTEGER,
    temperatura_mesa        INTEGER,
    ativo                   BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ck_filamentos_material CHECK (
        material IN ('PLA', 'ABS', 'PETG', 'TPU', 'ASA', 'NYLON', 'RESINA', 'OUTRO')
    ),
    CONSTRAINT ck_filamentos_custo CHECK (custo_por_kg >= 0),
    CONSTRAINT ck_filamentos_estoque CHECK (quantidade_estoque_g >= 0)
);

CREATE INDEX idx_filamentos_material ON filamentos (material);

COMMENT ON TABLE filamentos IS 'Filamentos/resinas com controle de estoque em gramas';
