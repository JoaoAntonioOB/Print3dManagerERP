-- =====================================================================
-- V10 — Refresh tokens
-- Tokens opacos (UUID) persistidos com rotação: a cada uso o token
-- antigo é revogado e um novo é emitido. Vários tokens ativos por
-- usuário são permitidos (multissessão).
-- =====================================================================

CREATE TABLE refresh_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT          NOT NULL,
    token           UUID            NOT NULL,
    expira_em       TIMESTAMPTZ     NOT NULL,
    revogado        BOOLEAN         NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens (usuario_id);

COMMENT ON TABLE refresh_tokens IS 'Refresh tokens JWT com rotação e revogação';
