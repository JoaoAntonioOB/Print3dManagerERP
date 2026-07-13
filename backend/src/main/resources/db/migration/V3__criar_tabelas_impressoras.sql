-- =====================================================================
-- V3 — Impressoras e configurações de custo
-- A configuração com impressora_id NULL é a configuração padrão global
-- (usada no cálculo de orçamentos quando a impressora não tem a sua).
-- =====================================================================

CREATE TABLE impressoras (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                    VARCHAR(120)    NOT NULL,
    marca                   VARCHAR(80),
    modelo                  VARCHAR(80),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DISPONIVEL',
    potencia_watts          INTEGER,
    volume_x_mm             INTEGER,
    volume_y_mm             INTEGER,
    volume_z_mm             INTEGER,
    horas_impressao_total   NUMERIC(10,2)   NOT NULL DEFAULT 0,
    valor_aquisicao         NUMERIC(12,2),
    data_aquisicao          DATE,
    observacoes             TEXT,
    ativo                   BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ck_impressoras_status CHECK (
        status IN ('DISPONIVEL', 'IMPRIMINDO', 'EM_MANUTENCAO', 'INATIVA')
    )
);

CREATE TABLE configuracoes_impressora (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    impressora_id       BIGINT,
    valor_kwh           NUMERIC(10,4)   NOT NULL,
    valor_hora_maquina  NUMERIC(10,2)   NOT NULL,
    custo_desgaste_hora NUMERIC(10,2)   NOT NULL DEFAULT 0,
    markup_padrao       NUMERIC(5,2)    NOT NULL DEFAULT 100.00,
    criado_em           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_config_impressora UNIQUE (impressora_id),
    CONSTRAINT fk_config_impressora FOREIGN KEY (impressora_id)
        REFERENCES impressoras (id) ON DELETE CASCADE,
    CONSTRAINT ck_config_valores CHECK (
        valor_kwh >= 0 AND valor_hora_maquina >= 0
        AND custo_desgaste_hora >= 0 AND markup_padrao >= 0
    )
);

-- Garante no máximo UMA configuração global (impressora_id NULL)
CREATE UNIQUE INDEX uk_config_global
    ON configuracoes_impressora ((impressora_id IS NULL))
    WHERE impressora_id IS NULL;

COMMENT ON TABLE  configuracoes_impressora               IS 'Parâmetros de custo usados no cálculo de orçamentos';
COMMENT ON COLUMN configuracoes_impressora.impressora_id IS 'NULL = configuração padrão global';
COMMENT ON COLUMN configuracoes_impressora.markup_padrao IS 'Margem de lucro padrão em porcentagem (ex.: 100.00 = 100%)';
