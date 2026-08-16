## Alteração de schema — Índices para hot paths (achado ALTO #10 da auditoria)

**Migração criada:** `V13__indices_hot_paths.sql`

**Tabelas/colunas afetadas:**
- `historico_impressoes.finalizado_em` → novo índice `idx_historico_finalizado_em`.
- `transacoes_financeiras.pedido_id` → novo índice `idx_transacoes_pedido_id` (não-parcial, complementar ao índice único parcial já existente).

**Entidades JPA atualizadas:** nenhuma — índices não são mapeados nas entidades JPA; `PrintHistory` e `FinancialTransaction` permanecem válidos contra o schema (`ddl-auto: validate` não valida índices, só colunas/tipos/constraints mapeadas).

**Detalhamento da análise (passo 2 da tarefa):**
- Confirmado em `V8__criar_tabela_historico_impressoes.sql`: coluna `finalizado_em TIMESTAMPTZ` sem índice. Usada em `ReportQueryRepository` (consumo de filamento por período: `WHERE h.finalizado_em >= :inicio AND h.finalizado_em < :fim` e `consumoFilamentoPorMes` com `finalizado_em >= :inicio GROUP BY to_char(finalizado_em, 'YYYY-MM')`) — hot path real de relatórios/dashboard confirmado por leitura do código, não só do enunciado.
- Confirmado em `V9__criar_tabela_transacoes_financeiras.sql`: coluna `pedido_id BIGINT` sem índice dedicado (só a FK, sem índice em Postgres por padrão).
- Confirmado em `V12__unique_receita_ativa_por_pedido.sql`: já existe `uq_transacoes_receita_ativa_por_pedido`, índice único **parcial** `ON transacoes_financeiras (pedido_id) WHERE pedido_id IS NOT NULL AND tipo = 'RECEITA' AND status <> 'CANCELADA'`.
  - Esse parcial cobre exatamente a consulta de `OrderBillingService.jaFaturado()` → `FinancialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot(pedidoId, RECEITA, CANCELADA)` (mesmo filtro), então **não** foi duplicado para esse caso.
  - Porém `FinancialTransactionSpecifications.comFiltros(...)` (usada em `FinancialTransactionService.listar`, endpoint de listagem de transações) filtra por `pedidoId` **sem** restrição de `tipo`/`status` — inclui `DESPESA` e receitas `CANCELADA`, que ficam fora do índice parcial. Essa é a consulta usada pela tela de detalhe do pedido/financeiro para listar todas as transações vinculadas a um pedido.
  - Conclusão: índice parcial existente é insuficiente para esse caso → adicionado `idx_transacoes_pedido_id` (não-parcial) como complemento, sem remover ou alterar o índice único da V12.

**É destrutiva/perde dado?** Não. Apenas `CREATE INDEX` (nenhum `DROP`/alteração de tipo/dado).

**Achados (severidade):** nenhum novo — migração resolve o achado ALTO #10 apontado pela auditoria.

**Validação:**
- Compilação: `cd backend && mvnw.cmd -B compile` → `BUILD SUCCESS` (JDK 21, `JAVA_HOME=C:\Program Files\Java\jdk-21.0.10`).
- Boot local / aplicação da migração: `mvnw.cmd -B test` com Docker Desktop ativo (Testcontainers sobe Postgres 16 e aplica todas as migrações do zero). Log do Flyway confirma: `Successfully validated 13 migrations` e `Current version of schema "public": 13` — **V13 aplicou limpo**.
- Suíte de testes: 108 testes executados, **4 failures + 3 errors**, todos em `PrintHistoryServiceTest` (`order` domain — `finalizarRejeitaEstoqueInsuficiente`, `iniciarExigePedidoEmProducao`, `iniciarRejeitaImpressoraDesativada`, `iniciarRejeitaImpressoraOcupada`, `concluirCalculaCustoEConsomeEstoque`, `falharConsomeEstoqueERegistraMotivo`, `iniciarOcupaImpressora`), lançando `ResourceNotFoundException` em vez do `BusinessException` esperado.
  - **Não relacionado a esta migração.** `git status` no momento da execução mostra `PrintHistoryService.java`, `PrintHistoryServiceTest.java` e `PrintHistoryRepository.java` como **modificados por outro agente rodando em paralelo** (mesmo cenário de concorrência mencionado no meu escopo de tarefa — "outros agentes corrigindo achados diferentes"). Não toquei em nenhum arquivo do domínio `order`/`printhistory`; apenas criei a migração SQL V13. Recomendo que o agente responsável por essas mudanças (provavelmente relacionado a estoque/filamento na conclusão de impressão) reexecute a suíte após estabilizar seu próprio trabalho.

**Colisão de versão:** nenhuma — `V13` estava livre no momento da criação (última migração existente era `V12__unique_receita_ativa_por_pedido.sql`).
