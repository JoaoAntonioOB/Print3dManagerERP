## Testes — Auditoria de cobertura (Fase 1, sem escrita de código)

**Testes criados/alterados:** nenhum (rodada de auditoria pura — nenhum teste foi criado ou alterado, conforme instrução desta rodada).

**Resultado da suíte:** **passou** — 104/104 testes verdes, 0 falhas, 0 erros, 0 skipped.
Comando usado: `cd backend && $env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'; .\mvnw.cmd -B test`.
Docker Desktop não estava rodando no início da sessão; foi iniciado manualmente (`Docker Desktop.exe`) e aguardado até ficar pronto (`docker version` respondeu, Engine 29.6.1) antes de rodar a suíte completa — portanto os 13 testes de integração (Testcontainers, Postgres 16-alpine) também foram executados de fato, não pulados. Tempo total: ~1min16s.

---

### Confirmação dos dois pontos cross-domain pedidos

**1. Condição de corrida no faturamento (`erp-audit.md`, achado CRÍTICO em `OrderBillingService`, e ALTO em `QuoteService.converter`) — confirmado: não há nenhum teste de concorrência real na suíte.**
- Busquei por `Thread|ExecutorService|CountDownLatch|CompletableFuture|@RepeatedTest|Concurrent` em toda `backend/src/test/java` — **zero ocorrências** em qualquer arquivo da suíte.
- Li `OrderBillingServiceTest.java` (`backend/src/test/java/com/print3dmanager/erp/financial/service/OrderBillingServiceTest.java`) linha a linha: todos os 8 métodos são Mockito puro (`@ExtendWith(MockitoExtension.class)`), stubs sequenciais de `orderRepository.findById`/`financialTransactionRepository.existsByPedidoIdAndTipoAndStatusNot` — nunca duas threads reais concorrendo contra o mesmo mock ou contra o Postgres. Cobre bem os caminhos de decisão sequenciais (409 duplicado, 400 sem valor/status inválido, 404, silêncio da entrega automática), mas por construção **não pode** pegar a janela TOCTOU entre o `SELECT exists` e o `INSERT` que o `erp-audit` descreveu.
- `QuoteService.converter` (`backend/src/main/java/.../quote/service/QuoteService.java:177`) não tem nenhum teste unitário dedicado (não existe `QuoteServiceTest.java` na suíte — só `CostMarkupPricingStrategyTest`, que testa a *strategy* de precificação, não o `QuoteService` em si) e o único teste de integração que toca a conversão (`QuoteFlowIntegrationTest`) exercita **uma** chamada `POST /quotes/{id}/converter` sequencial, não duas concorrentes.
- **Conclusão confirmada:** o achado CRÍTICO/ALTO do `erp-audit` é real e a suíte de 104 testes hoje não pegaria nenhuma das duas corridas.

**2. `FilamentServiceTest`/`PrinterServiceTest` (`printing3d-audit.md`) — confirmado: não existem.**
- Listagem completa de `backend/src/test/java/com/print3dmanager/erp/**/*.java` (18 classes de teste) não contém `FilamentServiceTest` nem `PrinterServiceTest` em lugar nenhum.
- Comparando com os services reais em `backend/src/main/java` (`FilamentService`, `PrinterService`, além de `ClientService`, `DashboardService`, `InventoryItemService`, `QuoteService`, `ReportService`, `UserService`), **nenhum destes tem teste unitário dedicado** — a lacuna é mais ampla do que só o par citado pelo `printing3d-audit`.

---

**Lacunas de cobertura identificadas** (além das duas confirmadas acima):

1. **Nenhum teste unitário para 8 services inteiros**: `ClientService`, `DashboardService`, `FilamentService`, `InventoryItemService`, `PrinterService`, `QuoteService`, `ReportService`, `UserService`. Para módulos com máquina de estados/regra de negócio própria isso é especialmente relevante: `QuoteService` (ciclo RASCUNHO/ENVIADO/APROVADO/REJEITADO/EXPIRADO/CONVERTIDO, número sequencial `ORC-`, herança de markup) e `FilamentService`/`InventoryItemService` (movimentação de estoque ENTRADA/SAÍDA, bloqueio de saldo negativo, filtro `estoqueBaixo`) só são exercitados indiretamente via `QuoteFlowIntegrationTest` (3 cenários) e nunca via `InventoryItemService`.
2. **`FinancialTransactionService` sem teste dedicado**: só `OrderBillingService` (o serviço "vizinho" de faturamento de pedidos) tem teste; o CRUD/máquina de estados PENDENTE→PAGA/CANCELADA, estorno PAGA→PENDENTE, bloqueio de PUT/DELETE fora de PENDENTE, e o cadastro com `status=CANCELADA` rejeitado — nada disso tem teste unitário ou de integração.
3. **11 dos 15 controllers não têm nenhum teste de integração** (`ClientController`, `DashboardController`, `FilamentController`, `FinancialTransactionController`, `InventoryItemController`, `PrinterConfigurationController`, `PrinterController`, `PrintHistoryController`, `ReportController`, `UserController`, `PublicQuoteController` só é coberto indiretamente via `QuoteFlowIntegrationTest`). Só `Order`, `OrderItemFile`, `Quote` e `Auth` têm `*FlowIntegrationTest`.
4. **Autorização (401/403) testada uma única vez, para uma única rota**: `AuthFlowIntegrationTest.perfilSemPermissaoRecebe403` cobre só `GET /users` (ADMINISTRADOR-only). Nenhum teste de integração exercita:
   - a fronteira `PODE_GERENCIAR` (ADMIN/OPERADOR) vs `PODE_CONSULTAR` (+FINANCEIRO/VISUALIZADOR, sem CLIENTE) usada em Clientes/Filamentos/Estoque/Impressoras;
   - a fronteira específica do Financeiro (ADMIN/FINANCEIRO gerenciam; OPERADOR/VISUALIZADOR só consultam) — inclusive `POST /orders/{id}/faturar`, documentado como "só ADMINISTRADOR/FINANCEIRO", nunca testado quanto a 403 para outros papéis;
   - a restrição de `/reports/financeiro` a ADMINISTRADOR/FINANCEIRO;
   - o papel `CLIENTE` nunca aparece em nenhum teste (nem para confirmar que é bloqueado dos endpoints internos).
5. **Concorrência de estoque de filamento e ocupação de impressora** (achados ALTO 1 e 2 do `printing3d-audit`, em `FilamentService.movimentarEstoque`/`PrintHistoryService.consumirFilamento` e `PrintHistoryService.iniciar`): mesma lacuna estrutural do item 1 — zero teste de concorrência real em toda a suíte, então essas duas corridas também não são pegas.
6. **`PATCH /printers/{id}/status` e soft delete de impressora com job `EM_ANDAMENTO`** (achado ALTO 3 do `printing3d-audit`): não há teste (unitário ou integração) que crie um `PrintHistory EM_ANDAMENTO` e então tente mudar o status/desativar a impressora — a ausência de checagem não é coberta em nenhuma direção (nem para confirmar bug, nem para prevenir regressão futura caso vire regra).
7. `Print3dManagerErpApplicationTests` cobre boot + 11 migrações + admin da V11, mas não há teste de integração que valide a ausência de índice único em `transacoes_financeiras.pedido_id` citada pelo `erp-audit` (não haveria como um teste de schema pegar isso sem ser um teste de constraint dedicado).

**Bugs reais encontrados (severidade):** nenhum novo nesta rodada — o escopo foi confirmar, via leitura de teste e execução real da suíte, dois achados já reportados por `erp` (CRÍTICO: duplicação de receita) e `printing3d` (ALTO ×3: corrida de estoque/impressora, status vs job em andamento). Ambos **confirmados como reais e não cobertos pela suíte atual** (ver seção acima) — a suíte passar 104/104 não contradiz esses achados, porque nenhum teste hoje exercita os cenários de concorrência ou de transição de estado que os revelam.

**Encaminhamentos:**
- `erp`: os achados CRÍTICO (`OrderBillingService`) e ALTO (`QuoteService.converter`) de condição de corrida seguem sem cobertura de teste — se a correção (advisory lock / constraint única) for implementada, este agente (QA) deve então escrever um teste de integração com `ExecutorService`/`CountDownLatch` disparando 2 requisições reais concorrentes contra o Testcontainers para validar a correção (não é possível validar isso com Mockito sequencial).
- `printing3d`: idem para os 2 achados ALTO de corrida (estoque de filamento, ocupação de impressora) e o achado ALTO 3 (status/soft-delete de impressora ignorando job `EM_ANDAMENTO`) — depois de uma decisão de regra de negócio (bloquear vs. cancelar automaticamente, conforme o próprio `printing3d-audit` já encaminhou a `erp`/`backend`), este agente escreve o teste correspondente.
- `backend`/`erp`: considerar priorizar `FinancialTransactionServiceTest` (máquina de estados sem nenhuma cobertura hoje) e testes de integração para os 11 controllers sem `*FlowIntegrationTest`, especialmente as fronteiras de autorização por papel (item 4) — hoje um endpoint mal anotado com `@PreAuthorize` erroneamente permissivo não seria pego por nenhum teste existente.
- Nenhum encaminhamento de correção de produção partiu desta auditoria — papel do QA aqui foi só confirmar lacunas e rodar a suíte real.

---

Arquivos relevantes lidos/verificados (caminhos absolutos):
- `C:\repository\Print3d Manager ERP\PROJECT_CONTEXT.md` (seção "Testes — Etapa 18")
- `C:\repository\Print3d Manager ERP\backend\src\test\java\com\print3dmanager\erp\financial\service\OrderBillingServiceTest.java`
- `C:\repository\Print3d Manager ERP\backend\src\test\java\com\print3dmanager\erp\security\auth\AuthFlowIntegrationTest.java`
- `C:\repository\Print3d Manager ERP\backend\src\main\java\com\print3dmanager\erp\quote\service\QuoteService.java`
- `C:\repository\Print3d Manager ERP\.claude\audits\erp-audit.md`, `.claude\audits\printing3d-audit.md`
- Diretórios completos `backend\src\test\java` (18 classes de teste) e `backend\src\main\java\**\service` (18 services) comparados para identificar as lacunas listadas.
