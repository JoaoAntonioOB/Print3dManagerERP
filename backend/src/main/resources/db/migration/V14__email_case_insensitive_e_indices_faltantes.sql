-- =====================================================================
-- V14 — E-mail de usuário case-insensitive + índices faltantes
-- Achados MÉDIOS da auditoria (.claude/audits/FINAL-PLAN.md, #15 e #20).
-- =====================================================================

-- #15 — usuarios.email era único, mas case-sensitive: "Ana@print3d.com"
-- e "ana@print3d.com" conseguiam cadastrar duas contas diferentes, e o
-- login por e-mail digitado com caixa distinta da do cadastro podia
-- resultar em 401 (dependendo de como a busca comparasse as strings).
-- Troca a constraint simples por um índice único funcional sobre
-- LOWER(email), e a busca no lado Java (UserRepository/DatabaseUserDetailsService)
-- passa a usar IgnoreCase para casar com essa unicidade.
--
-- Observação: não há dados de produção nesta base ainda (schema em
-- desenvolvimento/TCC) — não é esperado nenhum e-mail duplicado por
-- caixa hoje. Caso este script rode contra uma base com dados reais no
-- futuro, o CREATE UNIQUE INDEX abaixo falhará se já existirem dois
-- usuários cujo e-mail difere só em maiúsculas/minúsculas; nesse caso é
-- necessário higienizar (mesclar ou renomear) os registros conflitantes
-- antes de reaplicar esta migração.
ALTER TABLE usuarios DROP CONSTRAINT uk_usuarios_email;

CREATE UNIQUE INDEX uk_usuarios_email_lower ON usuarios (LOWER(email));

COMMENT ON INDEX uk_usuarios_email_lower IS
    'Unicidade de e-mail case-insensitive (substitui a antiga uk_usuarios_email); a busca por e-mail no login/cadastro também é IgnoreCase no UserRepository.';

-- #20 — índices faltantes adicionais, complementando os já criados na V13.
CREATE INDEX idx_impressoras_status ON impressoras (status);
CREATE INDEX idx_historico_filamento_id ON historico_impressoes (filamento_id);
CREATE INDEX idx_historico_item_pedido_id ON historico_impressoes (item_pedido_id);
CREATE INDEX idx_transacoes_cliente_id ON transacoes_financeiras (cliente_id);

COMMENT ON INDEX idx_impressoras_status IS
    'Suporta o filtro por status na listagem de impressoras e a checagem de disponibilidade ao iniciar impressão.';
COMMENT ON INDEX idx_historico_filamento_id IS
    'Suporta agregações por filamento (relatório de consumo) e o filtro de impressões por filamento.';
COMMENT ON INDEX idx_historico_item_pedido_id IS
    'Suporta o filtro de impressões por item de pedido e a checagem de job em andamento por item.';
COMMENT ON INDEX idx_transacoes_cliente_id IS
    'Suporta o filtro por clienteId na listagem de transações financeiras.';
