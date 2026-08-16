-- =====================================================================
-- V13 — Índices para hot paths de consulta
-- Achado ALTO da auditoria (.claude/audits/FINAL-PLAN.md, #10): duas
-- colunas usadas em consultas frequentes não tinham índice dedicado.
-- =====================================================================

-- historico_impressoes.finalizado_em: filtrada/agregada por período em
-- ReportQueryRepository (consumo de filamento por período e por mês) e
-- no Dashboard — sem índice, cada consulta faz seq scan na tabela toda.
CREATE INDEX idx_historico_finalizado_em ON historico_impressoes (finalizado_em);

-- transacoes_financeiras.pedido_id: a V12 já criou um índice único
-- parcial (uq_transacoes_receita_ativa_por_pedido) cobrindo apenas
-- tipo = 'RECEITA' AND status <> 'CANCELADA', que é o filtro exato de
-- OrderBillingService.jaFaturado()/existsByPedidoIdAndTipoAndStatusNot.
-- Esse índice parcial NÃO cobre a listagem geral de transações por
-- pedido em FinancialTransactionSpecifications.comFiltros (filtro por
-- pedidoId sem restrição de tipo/status — inclui DESPESA e receitas
-- CANCELADA), que também é consulta frequente da tela de detalhe do
-- pedido/financeiro. Índice complementar não-parcial para esse caso.
CREATE INDEX idx_transacoes_pedido_id ON transacoes_financeiras (pedido_id);

COMMENT ON INDEX idx_historico_finalizado_em IS
    'Suporta filtros/agregações por período de finalização de impressões (relatórios e dashboard).';
COMMENT ON INDEX idx_transacoes_pedido_id IS
    'Suporta busca geral por pedido_id (qualquer tipo/status); complementa o índice único parcial da V12, que só cobre receita ativa.';
