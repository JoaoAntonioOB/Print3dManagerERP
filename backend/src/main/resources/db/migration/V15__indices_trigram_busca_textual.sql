-- =====================================================================
-- V15 — Índices trigram (pg_trgm) para busca textual com wildcard
-- Achado BAIXO da auditoria (.claude/audits/FINAL-PLAN.md, #29): a busca
-- rápida do cabeçalho (Ctrl+K, frontend/src/layout/BuscaRapida.tsx)
-- dispara 3 consultas simultâneas (pedidos, clientes, filamentos), cada
-- uma usando LOWER(coluna) LIKE '%termo%' nas respectivas Specifications
-- (ClientSpecifications, FilamentSpecifications, OrderSpecifications).
-- Wildcard à esquerda ('%termo%') não pode usar um índice B-tree comum
-- (idx_clientes_nome, criado na V2, não ajuda aqui) — cada tecla digitada
-- (mesmo com debounce) força sequential scan nas 3 tabelas.
--
-- A extensão pg_trgm decompõe o texto em trigramas e permite índices GIN
-- que aceleram LIKE/ILIKE com wildcard em qualquer posição. Os índices
-- abaixo são funcionais sobre LOWER(coluna), casando exatamente com a
-- expressão usada nas Specifications (cb.like(cb.lower(root.get(...)), ...)).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ClientSpecifications.comFiltros: busca por nome, email e cpf_cnpj.
CREATE INDEX idx_clientes_nome_trgm ON clientes USING gin (LOWER(nome) gin_trgm_ops);
CREATE INDEX idx_clientes_email_trgm ON clientes USING gin (LOWER(email) gin_trgm_ops);
CREATE INDEX idx_clientes_cpf_cnpj_trgm ON clientes USING gin (LOWER(cpf_cnpj) gin_trgm_ops);

-- FilamentSpecifications.comFiltros: busca por nome, marca e cor.
CREATE INDEX idx_filamentos_nome_trgm ON filamentos USING gin (LOWER(nome) gin_trgm_ops);
CREATE INDEX idx_filamentos_marca_trgm ON filamentos USING gin (LOWER(marca) gin_trgm_ops);
CREATE INDEX idx_filamentos_cor_trgm ON filamentos USING gin (LOWER(cor) gin_trgm_ops);

-- OrderSpecifications.comFiltros: busca por numero (o outro termo do OR,
-- nome do cliente via join, já é coberto por idx_clientes_nome_trgm acima —
-- mesma expressão LOWER(clientes.nome), reaproveitada pelo planner).
CREATE INDEX idx_pedidos_numero_trgm ON pedidos USING gin (LOWER(numero) gin_trgm_ops);

COMMENT ON INDEX idx_clientes_nome_trgm IS
    'Trigram (pg_trgm) sobre LOWER(nome): acelera LIKE ''%termo%'' em ClientSpecifications.comFiltros e no join de OrderSpecifications/QuoteSpecifications por nome do cliente, evitando seq scan a cada busca.';
COMMENT ON INDEX idx_clientes_email_trgm IS
    'Trigram (pg_trgm) sobre LOWER(email): acelera LIKE ''%termo%'' em ClientSpecifications.comFiltros.';
COMMENT ON INDEX idx_clientes_cpf_cnpj_trgm IS
    'Trigram (pg_trgm) sobre LOWER(cpf_cnpj): acelera LIKE ''%termo%'' em ClientSpecifications.comFiltros.';
COMMENT ON INDEX idx_filamentos_nome_trgm IS
    'Trigram (pg_trgm) sobre LOWER(nome): acelera LIKE ''%termo%'' em FilamentSpecifications.comFiltros.';
COMMENT ON INDEX idx_filamentos_marca_trgm IS
    'Trigram (pg_trgm) sobre LOWER(marca): acelera LIKE ''%termo%'' em FilamentSpecifications.comFiltros.';
COMMENT ON INDEX idx_filamentos_cor_trgm IS
    'Trigram (pg_trgm) sobre LOWER(cor): acelera LIKE ''%termo%'' em FilamentSpecifications.comFiltros.';
COMMENT ON INDEX idx_pedidos_numero_trgm IS
    'Trigram (pg_trgm) sobre LOWER(numero): acelera LIKE ''%termo%'' em OrderSpecifications.comFiltros (busca rápida do cabeçalho, Ctrl+K).';
