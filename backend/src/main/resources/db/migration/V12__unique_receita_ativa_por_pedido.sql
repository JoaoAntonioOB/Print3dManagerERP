-- =====================================================================
-- V12 — Impede duplicidade de receita por pedido
-- Regra de negócio: um pedido pode ter no máximo uma transação
-- RECEITA não cancelada vinculada a ele (ver OrderBillingService.
-- jaFaturado). Sem essa constraint, duas requisições concorrentes de
-- faturamento (duplo clique, faturamento manual + automático na
-- entrega) podiam passar pelo check-then-act da aplicação ao mesmo
-- tempo e cada uma criar sua própria receita, duplicando o valor nos
-- resumos/relatórios/dashboard.
--
-- Índice único parcial (não uma UNIQUE simples na coluna) porque:
--   - pedido_id é opcional (despesas e receitas avulsas não têm pedido);
--   - transações DESPESA não entram nessa regra;
--   - uma receita CANCELADA não bloqueia um novo faturamento do mesmo
--     pedido (comportamento já validado por jaFaturado()).
-- =====================================================================

CREATE UNIQUE INDEX uq_transacoes_receita_ativa_por_pedido
    ON transacoes_financeiras (pedido_id)
    WHERE pedido_id IS NOT NULL
      AND tipo = 'RECEITA'
      AND status <> 'CANCELADA';

COMMENT ON INDEX uq_transacoes_receita_ativa_por_pedido IS
    'No máximo uma receita não cancelada por pedido — última barreira contra faturamento duplicado sob concorrência.';
