## Alteração — 3 achados ALTO isolados (relatórios/report, JWT/security config, GlobalExceptionHandler/common)

**Arquivos alterados/criados:**
- `backend/src/main/java/com/print3dmanager/erp/report/service/ReportService.java` — teto de 366 dias no período dos relatórios.
- `backend/src/main/resources/application-prod.yml` — sobrescrita de `application.security.jwt.secret` sem default em produção.
- `backend/src/main/java/com/print3dmanager/erp/common/exception/GlobalExceptionHandler.java` — handler de `DataIntegrityViolationException` → 409.
- `backend/src/test/java/com/print3dmanager/erp/report/service/ReportServiceTest.java` (novo) — testes do teto de período.
- `backend/src/test/java/com/print3dmanager/erp/common/exception/DataIntegrityViolationIntegrationTest.java` (novo) — teste de integração do handler 409.

**Padrão seguido de:** módulo `report/` (Etapa 16) para o teto de período; `application-dev.yml`/`application-prod.yml` (padrão já usado em `DATABASE_PASSWORD`) para o JWT secret; estilo dos handlers `handleConflito`/`handleRegraDeNegocio` já existentes em `GlobalExceptionHandler` para o novo handler de integridade.

### Achado ALTO #7 — relatórios PDF sem teto de período
`ReportService.resolverPeriodo()` agora rejeita, com `BusinessException` (→ 400 pelo handler global), qualquer período cujo intervalo (`de`..`ate`, inclusive) exceda `MAX_DIAS_PERIODO = 366` dias. A validação está dentro do próprio `resolverPeriodo`, então os 3 métodos (`pedidos`, `financeiro`, `consumoFilamento`) ficam cobertos automaticamente sem duplicação. Mensagem: "O período do relatório não pode exceder 366 dias."

### Achado ALTO #8 — `JWT_SECRET` com fallback em produção
Decisão tomada após conferir o padrão real de `DATABASE_PASSWORD`: a base `application.yml` mantém seu default (`application.security.jwt.secret: ${JWT_SECRET:cHJpbnQz...}`) intacto, porque o perfil de teste (`application-test.yml`, ativado sozinho via `@ActiveProfiles("test")` nos testes de integração — sem herdar `dev`) depende dele; removê-lo do arquivo base quebraria a suíte Testcontainers sem que ninguém setasse `JWT_SECRET` nos testes. Em vez disso, segui a mesma técnica de merge de perfil Spring usada para `server.forward-headers-strategy` no próprio `application-prod.yml`: adicionei uma sobrescrita `application.security.jwt.secret: ${JWT_SECRET}` **sem default** só no perfil `prod`, que tem prioridade sobre a base quando o perfil `prod` está ativo. Resultado: dev/test continuam funcionando sem configurar nada; produção falha rápido na subida (`PlaceholderResolutionException`) se `JWT_SECRET` não estiver setado, no mesmo espírito de `spring.datasource.password` em `application-prod.yml`.
- Não toquei em `application-dev.yml` (o default de conveniência já vem da base, e duplicá-lo lá seria redundante já que a base cobre dev/test igualmente).

### Achado ALTO #9 — FKs sem `ON DELETE` geram 500 cru em vez de 409
Adicionado `@ExceptionHandler(DataIntegrityViolationException.class)` em `GlobalExceptionHandler`, posicionado logo após `handleConflito` (perto de `ResourceConflictException`, conforme pedido) e antes do handler genérico `Exception.class`. Responde 409 com mensagem genérica ("Não é possível excluir este registro: ele está vinculado a outros dados do sistema.") e loga em `WARN` (não `ERROR`, já que é uma condição de negócio esperada, não uma falha real do sistema) — mesmo padrão de `construir(...)` dos demais handlers.

**Build/testes:** compilou (`mvnw compile`, Java 21). Suíte completa rodada com `mvnw test` (Docker Desktop ativo, Testcontainers real): **todos os testes passaram**, incluindo os 2 novos arquivos:
- `ReportServiceTest` — 3/3 (período ≤ 366 dias aceito; período > 366 dias rejeitado tanto em `pedidos` quanto em `consumoFilamento`).
- `DataIntegrityViolationIntegrationTest` — 1/1 (cria pedido PENDENTE + transação financeira manual vinculada via `pedidoId`; `DELETE /orders/{id}` — que antes estourava 500 por violar `fk_transacoes_pedido`, sem `ON DELETE` — agora responde 409 com a mensagem esperada).

Único teste falho na suíte completa: `com.print3dmanager.erp.printer.service.PrinterServiceTest` (`Unresolved compilation problem: construtor PrinterResponse(...) indefinido`) — **não relacionado a nenhuma das minhas alterações**; é um teste do módulo `printer/`, que as restrições da tarefa proibiram explicitamente de tocar, e o erro de compilação (assinatura do record `PrinterResponse` mudou) indica edição concorrente de outro agente nesse mesmo módulo durante a execução. Reportado abaixo como achado a encaminhar, não corrigido por mim.

**Achados/riscos (severidade CRÍTICO/ALTO/MÉDIO/BAIXO/INFORMATIVO):**
- INFORMATIVO: `PrinterServiceTest` está com erro de compilação (assinatura de `PrinterResponse` incompatível com o teste) no momento em que rodei a suíte completa — não é resultado do meu trabalho; provavelmente edição concorrente em andamento no módulo `printer/`. Precisa ser corrigido por quem estiver mexendo em `PrinterService`/`PrinterResponse` antes do próximo `mvnw test` "oficial".
- BAIXO: o teto de 366 dias é fixo em código (`MAX_DIAS_PERIODO`), não configurável via `application.*`. Não criei propriedade tipada nova porque o achado pedia explicitamente um valor fixo ("ex.: 366 dias") sem mencionar necessidade de configuração externa; se o time quiser tornar isso ajustável por ambiente no futuro, é uma mudança pequena e isolada.

**Encaminhamentos para outros agentes:**
- `printer`/agente responsável por `PrinterService` e `PrinterResponse`: `PrinterServiceTest` está quebrado por incompatibilidade de assinatura do record `PrinterResponse` (erro de compilação, não de asserção) — provavelmente uma edição em andamento não finalizada em paralelo a esta tarefa. Precisa de atenção antes do próximo build oficial da suíte.
- Nenhum outro encaminhamento — as alterações deste agente não tocaram schema, `QuoteService`, `FilamentService`, `PrintHistoryService`, `PrinterService`, `docker-compose.yml` nem migrações Flyway, conforme restrição.
