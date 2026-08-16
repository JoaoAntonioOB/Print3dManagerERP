## Regra de negócio — corrida (check-then-act) em `QuoteService.converter()`

**Fluxo/máquina de estados afetada:** Ciclo de vida do Orçamento (`quote/`), transição terminal `APROVADO → CONVERTIDO` disparada por `POST /quotes/{id}/converter`, que reusa `OrderService.criar` para gerar o pedido.

**Transições antes → depois:** Nenhuma transição da máquina de estados foi adicionada, removida ou reordenada — `TRANSICOES` (RASCUNHO→ENVIADO; ENVIADO→RASCUNHO|APROVADO|REJEITADO|EXPIRADO; APROVADO/REJEITADO/EXPIRADO/CONVERTIDO terminais) permanece igual. A mudança foi só na **atomicidade** de como `converter()` verifica e aplica a transição `APROVADO → CONVERTIDO`:
- **Antes:** `Quote orcamento = obterOrcamentoDetalhado(id)` (carrega o estado no início da transação) → checa `status == APROVADO` → cria o pedido via `OrderService.criar` → marca `CONVERTIDO`. Duas chamadas concorrentes (duplo clique, retry de rede) para o mesmo orçamento podiam ler `APROVADO` simultaneamente antes de qualquer uma commitar, ambas passavam pela checagem e cada uma criava um pedido cobrável — corrida clássica de check-then-act, igual à já corrigida em `OrderBillingService`.
- **Depois:** `converter()` chama `quoteRepository.travarConversao(id)` (novo `pg_advisory_xact_lock(hashtext('orcamento_conversao_' || :orcamentoId))`) **antes** de carregar o orçamento. A segunda chamada concorrente fica bloqueada no lock até a primeira commitar (ou dar rollback); ao ser liberada, ela carrega o orçamento **já com o status commitado** pela vencedora (`CONVERTIDO`) e cai no branch de erro de negócio existente (`BusinessException`, "Somente orçamentos APROVADOS podem ser convertidos..." — mesmo texto de antes, agora também cobre a corrida). Nenhum pedido duplicado é criado.

**Impacto em faturamento/estoque:** Indireto, mas relevante — evita que a mesma peça/orçamento gere dois `Order` cobráveis (dois números `PED-*`, dois valores lançáveis no financeiro via faturamento manual ou automático na entrega). Não mexe em estoque de filamento nem em `PrintHistory`.

**Arquivos alterados:**
- `backend/src/main/java/com/print3dmanager/erp/quote/repository/QuoteRepository.java` — novo método `travarConversao(Long orcamentoId)`, mesmo padrão de `@Query(nativeQuery = true)` de `travarGeracaoNumero`/`travarFaturamento`, namespace de lock próprio (`orcamento_conversao_`) para não colidir com o namespace de geração de número (`orcamentos_numero_`).
- `backend/src/main/java/com/print3dmanager/erp/quote/service/QuoteService.java` — `converter()` agora adquire o lock antes de carregar o orçamento (em vez de carregar e checar antes do lock), para que a checagem de status enxergue o commit da conversão vencedora.
- `backend/src/test/java/com/print3dmanager/erp/quote/QuoteFlowIntegrationTest.java` — novo teste `conversaoConcorrenteGeraApenasUmPedido`: duas threads disparam `POST /quotes/{id}/converter` simultaneamente (sincronizadas por `CyclicBarrier`) contra o Postgres real do Testcontainers; asserta que os status HTTP são exatamente `{201, 400}` (uma vence, a outra recebe erro de negócio claro), que só existe **um** pedido para o cliente (`GET /orders?clienteId=`, `totalElements == 1`) e que o orçamento termina `CONVERTIDO`.

Sem migração Flyway nova (é lock transacional, não constraint de schema, conforme pedido).

**Testes relevantes rodados:**
- `mvnw compile` — sucesso.
- `mvnw -Dtest=QuoteFlowIntegrationTest test` — **4/4 passaram** (isolado), incluindo o novo teste de corrida.
- `mvnw test` (suíte completa) — **116/117 passaram**. O único erro é em `com.print3dmanager.erp.printer.service.PrinterServiceTest.alterarStatusAplicaSemJobAtivo` ("Unresolved compilation problem"), arquivo **não tocado por este trabalho** e confirmado como edição em andamento de outro agente em paralelo no módulo `printer/` (constatei `PrinterStatus.EM_MANUTENCAO` vs. o `MANUTENCAO` referenciado no teste ainda em transição, e `PrinterServiceTest.java`/`PrinterRepository.java`/`PrinterService.java` aparecem modificados/novos no `git status` fora deste escopo). `QuoteServiceTest` não existe no repositório (não há teste unitário Mockito dedicado a `QuoteService` hoje); a cobertura de `converter()` é via `QuoteFlowIntegrationTest` (HTTP + Postgres real), que é o ponto mais forte para validar semântica de `pg_advisory_xact_lock`.

**Achados (severidade):**
- Nenhum novo achado. Confirma-se o ALTO #3 do plano de auditoria e a correção aplicada segue exatamente o padrão já validado em `OrderBillingService`/`FinancialTransactionRepository`.

**Encaminhamentos:**
- Nenhum para este agente. Observação lateral (não é ação minha): há uma falha de compilação transitória em `PrinterServiceTest.java` (módulo `printer/`, fora do meu escopo) no momento desta execução — provável trabalho em andamento de outro agente (`printing3d`/`backend`) mexendo em `PrinterStatus`; não fiz nenhuma alteração ali por instrução explícita.
