-- =====================================================================
-- V2 — Clientes
-- Pessoa física ou jurídica. Pode ser vinculado a um usuário com
-- role CLIENTE (acesso ao portal de aprovação de orçamentos).
-- =====================================================================

CREATE TABLE clientes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(160)    NOT NULL,
    email           VARCHAR(160),
    telefone        VARCHAR(20),
    tipo_pessoa     VARCHAR(10)     NOT NULL DEFAULT 'FISICA',
    cpf_cnpj        VARCHAR(18),
    logradouro      VARCHAR(160),
    numero          VARCHAR(20),
    complemento     VARCHAR(80),
    bairro          VARCHAR(80),
    cidade          VARCHAR(80),
    estado          CHAR(2),
    cep             VARCHAR(9),
    observacoes     TEXT,
    usuario_id      BIGINT,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_clientes_cpf_cnpj UNIQUE (cpf_cnpj),
    CONSTRAINT uk_clientes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_clientes_tipo_pessoa CHECK (tipo_pessoa IN ('FISICA', 'JURIDICA'))
);

CREATE INDEX idx_clientes_nome ON clientes (nome);

COMMENT ON TABLE  clientes            IS 'Clientes da empresa (pessoa física ou jurídica)';
COMMENT ON COLUMN clientes.usuario_id IS 'Usuário com role CLIENTE vinculado (opcional)';
