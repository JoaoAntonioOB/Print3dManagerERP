## Auditoria final — Consolidação do ciclo de auditoria (9 domínios) sobre o estado atual do repositório (HEAD `c9aef25` — "Redesign do Dashboard + cabeçalho no estilo do mockup SaaS")

### Checklist
- [x] Build backend compila (`cd backend && $env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'; .\mvnw.cmd -B compile` → `BUILD SUCCESS`, confirmado ao vivo nesta auditoria)
- [x] Testes backend passam (`.\mvnw.cmd -B test` → `Tests run: 104, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, Docker Desktop ativo, Testcontainers/Postgres 16-alpine executados de fato — confirmado ao vivo)
- [x] Build/lint frontend passam (`npm run build` → exit 0, tsc+vite ok, bundle único 1.387 kB/gzip 405 kB com aviso de chunk >500kB; `npm run lint` → oxlint, exit 0 — confirmado ao vivo)
- [x] Segurança revisada (achados de `security-audit.md`/`devops-audit.md` confirmados por leitura direta: `docker-compose.yml` linhas 13 e 42 publicam `5432`/`8080`; `application.yml` linha 62 tem `JWT_SECRET` com default embutido em texto claro válido para todos os perfis, inclusive `prod`)
- [x] Regras de negócio íntegras — máquinas de estado (Pedido/Orçamento/Transação) corretas na lógica sequencial (confirmado por `erp-audit.md`, achado 4/INFORMATIVO), **mas** com janelas de condição de corrida reais e não testadas em fluxos financeiros/estoque/impressora (ver achados CRÍTICO/ALTO abaixo)
- [x] Sem regressão de performance óbvia introduzida nesta mudança (redesign de dashboard é só frontend) — achados de performance existentes (relatórios sem teto de período, `topClientes` sem janela) são pré-existentes, não introduzidos pelo commit mais recente
- [x] Documentação (`PROJECT_CONTEXT.md`) coerente com o estado real — seção 6 documenta a etapa mais recente (redesign do dashboard/cabeçalho) e diz explicitamente "sem mudança de backend"; confirmado por `git diff --stat HEAD` vazio (nenhuma mudança de código pendente de commit)
- [ ] Infraestrutura sobe sem quebra — **não verificado nesta rodada** (não subi a stack Docker completa eu mesmo); reutilizo a evidência recente e específica do `devops-audit.md` (`docker compose up -d --build` com postgres/backend/frontend healthy, `curl` 200/UP), que é a mesma versão de código desta auditoria (nenhum commit novo desde então) — aceito como válido por ausência de mudança de infraestrutura no meio tempo, mas não é uma re-execução própria

### Achados consolidados (todas as áreas, ordenados por severidade)

1. **[CRÍTICO]** Bypass total do rate limiting por spoofing de `X-Forwarded-For` via porta 8080 publicada — origem: infraestrutura (`docker-compose.yml`, confirmado nesta auditoria nas linhas 13/42) + segurança (`RemoteIpValve`/`RateLimitFilter`) — agente responsável: `devops` (remover `ports: ["8080:8080"]`/restringir a `127.0.0.1`; mesmo tratamento para a porta 5432 do achado 3 de segurança). **Nota de reconciliação exigida pelo orquestrador**: `security-audit.md` (achado 6) havia classificado esse mesmo fato (porta 8080 exposta) como BAIXO, concluindo que não via "vetor de bypass do rate limiting por spoofing" porque testou apenas com peer TCP externo genuíno. `devops-audit.md` testou a cadeia completa ao vivo — 13 requisições reais `POST /api/auth/login` direto em `localhost:8080` com `X-Forwarded-For` forjado e variável, confirmadas nos logs do backend como aceitas como IP remoto real, 13/13 sem 429 (vs. baseline correto de 429 na 11ª sem spoof). É a **mesma causa raiz** (porta 8080 publicada) vista por dois ângulos — o de `devops` é o correto porque foi validado empiricamente contra o cenário real (peer = gateway da bridge Docker, que cai nas faixas privadas que o `RemoteIpValve` trata como proxy interno), não teórico. Tratado aqui como **um único achado CRÍTICO**, não somado como dois achados independentes. Deriva total proteção contra força bruta de login e do link público de orçamento.

2. **[CRÍTICO]** Condição de corrida (TOCTOU) no faturamento de pedidos permite duplicação de receita — origem: `erp` (`financial/service/OrderBillingService.faturar`/`gerarReceitaSeNecessario`, check-then-act sem `pg_advisory_xact_lock` nem `@Version` nem unique constraint em `transacoes_financeiras.pedido_id`) — confirmado por `database-audit.md` (ausência de índice único, achado A2 relacionado) e por `qa-audit.md` (zero testes de concorrência em toda a suíte — busca por `Thread|ExecutorService|CountDownLatch|CompletableFuture|@RepeatedTest|Concurrent` deu zero ocorrências). Impacto financeiro direto e mensurável (receita duplicada em resumos/relatórios/dashboard). Agente responsável: `erp` (lock + constraint), depois `qa` (teste de concorrência real com Testcontainers).

3. **[ALTO]** Mesmo padrão de corrida na conversão de orçamento em pedido (`quote/service/QuoteService.converter`) — duplo clique pode gerar dois pedidos cobráveis para o mesmo orçamento aprovado — origem: `erp` — agente responsável: `erp`.

4. **[ALTO]** Condição de corrida no abate de estoque de filamento (lost update) — `FilamentService.movimentarEstoque`/`PrintHistoryService.consumirFilamento` sem lock, saldo final pode ficar maior que o real sob concorrência, mascarando consumo acima do estoque físico — origem: `printing3d` — agente responsável: `backend`/`database` (mecanismo de lock), coordenado com `printing3d`.

5. **[ALTO]** Condição de corrida na ocupação de impressora — `PrintHistoryService.iniciar` permite, sob concorrência real, dois jobs `EM_ANDAMENTO` na mesma máquina física (a garantia documentada só vale em uso sequencial) — origem: `printing3d` — agente responsável: `backend`/`database`.

6. **[ALTO]** `PATCH /printers/{id}/status` e soft delete de impressora não checam job `EM_ANDAMENTO` — permite ocupar/desativar impressora com job ativo "esquecido", nunca cancelado, custo/horas calculados sobre período que não reflete a realidade — origem: `printing3d` — agente responsável: `erp`/`backend` (decisão de bloquear vs. cancelar automaticamente).

7. **[ALTO]** Relatórios PDF sem teto de período — full scan + geração síncrona em memória para qualquer intervalo, sem limite no backend nem na UI — origem: `performance` — agente responsável: `backend` (teto + 400) e `frontend` (limitar seletor de datas).

8. **[ALTO]** `JWT_SECRET` com fallback silencioso ativo em produção (valor commitado em texto claro no repositório, sem fail-fast como já ocorre para credenciais do banco) — origem: `security` — agente responsável: `backend` (yml)/`devops` (docker-compose.yml).

9. **[ALTO]** Postgres publicado no host com senha default fraca previsível caso `.env` não seja criado — origem: `security`/`devops` — agente responsável: `devops` (coordenado com `database`).

10. **[ALTO]** Índices ausentes em hot paths reais: `historico_impressoes.finalizado_em` (dashboard/relatório) e `transacoes_financeiras.pedido_id` (checado a cada faturamento/entrega) — origem: `database` — agente responsável: `database`. (Nota: o índice em `pedido_id` também serviria de apoio parcial ao achado 2 acima, mas não substitui o lock/constraint transacional.)

11. **[ALTO]** FKs de `pedidos.id` sem `ON DELETE` colidem com exclusão física de pedido `PENDENTE` vinculado a orçamento convertido/transação manual → 500 cru (sem handler para `DataIntegrityViolationException`) — origem: `database` — agente responsável: `order`/`erp` (decisão de negócio) + `backend`/`database` (implementação).

12. **[MÉDIO]** Regressão do gotcha Jackson `non_null` em Estoque de insumos (`InventoryPage.tsx:228`, `!== null` em vez de `!= null`) — exibe "R$ NaN" quando custo unitário não é informado — origem: `frontend` — agente responsável: `frontend`.

13. **[MÉDIO]** `FilamentService`/`PrinterService`/`QuoteService`/`FinancialTransactionService` (entre outros 8 services) sem nenhum teste unitário — inclusive máquinas de estado reais sem cobertura — origem: `printing3d`/`qa` — agente responsável: `backend`/`qa`.

14. **[MÉDIO]** 11 de 15 controllers sem teste de integração; fronteiras de autorização por papel (`PODE_GERENCIAR`/`PODE_CONSULTAR`, Financeiro, `/reports/financeiro`) testadas uma única vez para uma única rota — origem: `qa` — agente responsável: `qa`.

15. **[MÉDIO]** `usuarios.email` único mas case-sensitive, sem normalização — permite contas duplicadas por caixa e 401 falso-negativo — origem: `database` — agente responsável: `user`/`security`.

16. **[MÉDIO]** `topClientes` do dashboard agrega histórico completo de pedidos sem janela de tempo, ao contrário das demais séries — origem: `performance` — agente responsável: `backend` coordenado com `erp`.

17. **[MÉDIO]** Reuso de refresh token detectado não derruba as demais sessões da "família" do token (falta chamar `revogarTodosDoUsuario` no caminho de reuso) — origem: `security` — agente responsável: `backend`.

18. **[MÉDIO]** `react-router-dom` com advisory de severidade alta (`npm audit`, GHSA-qwww-vcr4-c8h2) — exploração prática nula no modo SPA atual, mas CVE aberto em produção — origem: `security` — agente responsável: `frontend`.

19. **[MÉDIO]** Variáveis `RATE_LIMIT_*` documentadas no `.env.example` mas não repassadas pelo `docker-compose.yml` — configuração silenciosamente ignorada — origem: `devops` — agente responsável: `devops`.

20. **[MÉDIO]** Índices ausentes adicionais (`impressoras.status`, `historico_impressoes.filamento_id`/`item_pedido_id`, `transacoes_financeiras.cliente_id`) — origem: `database` — agente responsável: `database`.

21. **[BAIXO]** Upload STL/3MF validado só por extensão, sem magic bytes — avaliado formalmente por `security` como **aceitável como está** dado o conjunto de mitigações (auth interna, download sempre `attachment` com content-type fixo por extensão, path traversal bloqueado); hardening opcional não bloqueante — origem: `printing3d`/`security` — agente responsável: `backend` (se decidido implementar).
22. **[BAIXO]** `cancelar` de impressão pode abater estoque/gerar custo sem isso estar documentado explicitamente no `PROJECT_CONTEXT.md` — origem: `printing3d` — agente responsável: `tcc`/`printing3d` (documentação).
23. **[BAIXO]** Ausência de `Content-Security-Policy` no NGINX, agravando impacto de eventual XSS dado token em `localStorage` — origem: `security` — agente responsável: `devops`.
24. **[BAIXO]** Janela fixa do rate limiter permite rajada de ~2× o limite na fronteira — origem: `security` — agente responsável: `backend` (opcional).
25. **[BAIXO]** Política de senha com mínimo de 6 caracteres — origem: `security` — agente responsável: `backend`.
26. **[BAIXO]** Sem verificação automatizada de CVEs no pipeline (Maven/npm) — origem: `security` — agente responsável: `devops`.
27. **[BAIXO]** `OrderItemResponse.arquivoModelo` expõe caminho interno de armazenamento (UUID de anti-colisão) — origem: `backend` — agente responsável: `backend`.
28. **[BAIXO]** `CartaoAcao` do dashboard (`AcoesHoje.tsx`) não é acessível por teclado/leitor de tela — origem: `frontend` — agente responsável: `frontend`.
29. **[BAIXO]** Índices faltantes em buscas textuais (wildcard à esquerda, não indexável por btree comum) — amplificado 3× pela busca rápida do cabeçalho (`Ctrl+K`) — origem: `database`/`performance` — agente responsável: `database`.
30. **[BAIXO]** `AtividadeRecente` busca 20 clientes ordenados por nome só para aproximar "3 mais recentes" — imprecisão que piora com o crescimento da base — origem: `performance` — agente responsável: `frontend`.
31. **[BAIXO]** Bundle único do frontend >500kB sem code-splitting — origem: `frontend` — agente responsável: `frontend` (opcional).
32. **[INFORMATIVO]** Rate limiting em memória (instância única) — decisão consciente e adequada ao escopo do TCC, confirmada por `security`; não é achado de correção.
33. **[INFORMATIVO]** Cobertura estrutural (DTOs como records, `@PreAuthorize` explícito, N+1 tratado via `@EntityGraph`, Swagger 100%, enums↔CHECKs sincronizados, timestamps `TIMESTAMPTZ`/UTC) confirmada como exemplar em todos os 9 domínios — nenhum achado estrutural de severidade relevante.

### Veredito

**PRONTO COM RESSALVAS** — o build e os 104 testes backend passam de fato (confirmado ao vivo nesta auditoria), o frontend compila e lint fica limpo, e a mudança mais recente (redesign do dashboard/cabeçalho) é puramente visual, sem alteração de backend, consistente com a documentação. Porém a entrega carrega **dois achados CRÍTICOS reais e não cobertos por nenhum teste da suíte atual**: (1) bypass total do rate limiting de força bruta via porta 8080 exposta + spoofing de `X-Forwarded-For`, validado empiricamente com 13/13 requisições sem bloqueio (reconciliação `devops`×`security`: tratar como CRÍTICO, não BAIXO); e (2) duplicação de receita financeira por condição de corrida no faturamento de pedidos, sem lock nem constraint. Ambos exigem correção do agente `devops`/`erp` respectivamente antes de qualquer exposição real fora de ambiente controlado de demonstração — uma demo controlada (rede fechada, sem tentativa deliberada de duplo-clique/spoof) pode prosseguir, mas a entrega como "pronta para produção/uso comercial" (ambição declarada no `PROJECT_CONTEXT.md`) não deve avançar sem essas duas correções.
