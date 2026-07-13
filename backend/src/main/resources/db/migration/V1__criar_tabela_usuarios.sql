-- =====================================================================
-- V1 — Usuários do sistema
-- Autenticação (email + senha BCrypt) e autorização por papel (role).
-- =====================================================================

CREATE TABLE usuarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(120)    NOT NULL,
    email           VARCHAR(160)    NOT NULL,
    senha           VARCHAR(100)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_role CHECK (
        role IN ('ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR', 'CLIENTE')
    )
);

COMMENT ON TABLE  usuarios       IS 'Usuários do sistema com autenticação e papel de acesso';
COMMENT ON COLUMN usuarios.senha IS 'Hash BCrypt — nunca armazenar senha em texto plano';
