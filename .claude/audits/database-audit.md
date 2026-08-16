## Alteração de schema — e-mail case-insensitive (usuarios) + índices faltantes (achados MÉDIOS #15 e #20)

**Migração criada:** `V14__email_case_insensitive_e_indices_faltantes.sql`

**Tabelas/colunas afetadas:**
- `usuarios.email` — remove `CONSTRAINT uk_usuarios_email UNIQUE (email)`, cria `CREATE UNIQUE INDEX uk_usuarios_email_lower ON usuarios (LOWER(email))`.
- `impressoras.status` — novo índice `idx_impressoras_status`.
- `historico_impressoes.filamento_id` — novo índice `idx_historico_filamento_id`.
- `historico_impressoes.item_pedido_id` — novo índice `idx_historico_item_pedido_id`.
- `transacoes_financeiras.cliente_id` — novo índice `idx_transacoes_cliente_id`.

**Entidades JPA atualizadas:**
- `user/model/User.java` — removido `unique = true` da coluna `email` (a unicidade agora é garantida pelo índice funcional `uk_usuarios_email_lower`, não por uma unique constraint simples de coluna; comentário adicionado explicando).
- Nenhuma outra entidade precisou mudar: os novos índices não alteram tipo/nullability/relacionamento de nenhuma coluna já mapeada (`ddl-auto: validate` continua satisfeito, pois Hibernate em modo validate não valida índices).

**Mudanças no código Java (achado #15 — busca case-insensitive):**
- `user/repository/UserRepository.java`: `findByEmail` → `findByEmailIgnoreCase`; `existsByEmail` → `existsByEmailIgnoreCase`; `existsByEmailAndIdNot` → `existsByEmailIgnoreCaseAndIdNot` (convenção de nome do Spring Data JPA, sem `@Query` manual).
- `security/DatabaseUserDetailsService.java` (fluxo de login): passou a chamar `findByEmailIgnoreCase`.
- `security/DefaultAdminPasswordWarner.java`: idem (único outro call site direto do repository).
- `user/service/UserService.java`: `buscarPorEmail` usa `findByEmailIgnoreCase`; `criar`/`atualizar` usam `existsByEmailIgnoreCase(AndIdNot)` — cadastro/edição duplicados por caixa agora caem no 409 amigável (`ResourceConflictException`) antes de estourar a violação do índice único no banco.
- Testes ajustados para compilar com os novos nomes de método: `Print3dManagerErpApplicationTests`, `security/DefaultAdminPasswordWarnerTest` (mocks de `findByEmail` → `findByEmailIgnoreCase`).
- Não foi tocado nenhum service fora do escopo do achado (`FilamentService`, `PrinterService`, `QuoteService`, `OrderBillingService`, `ReportService`, `GlobalExceptionHandler`, `DashboardService` permanecem intocados — confirmado por `git diff --stat` restrito aos arquivos acima).

**É destrutiva/perde dado?** Não, no estado atual do banco (schema de desenvolvimento, sem dados de produção reais). A migração troca uma unique constraint simples por um índice único funcional equivalente (mais permissivo em relação a caixa, não menos) — nenhuma linha existente é removida ou truncada. Documentei no próprio comentário SQL da V14 que, se esta migração rodar no futuro contra uma base com e-mails reais que já colidem só por caixa (ex.: `Ana@print3d.com` e `ana@print3d.com`), o `CREATE UNIQUE INDEX` falhará e exigirá higienização manual dos registros conflitantes antes de reaplicar — não há isso aqui hoje.

**Achados (severidade):** nenhum novo — apenas os dois achados MÉDIOS (#15, #20) do plano de auditoria, ambos corrigidos nesta migração.

**Validação:**
- `cd backend && .\mvnw.cmd -B compile` → compilou sem erros.
- `cd backend && .\mvnw.cmd -B test` (JDK 21, Docker Desktop ativo, Testcontainers) → **141 de 142 testes verdes**, incluindo:
  - `Print3dManagerErpApplicationTests` (Flyway "Successfully validated 14 migrations", schema em v14 — boot local aplicou a V14 limpa).
  - `security.DefaultAdminPasswordWarnerTest`, `security.auth.AuthFlowIntegrationTest` (login) exercitando os métodos renomeados.
  - `filament.repository.FilamentRepositoryLockIntegrationTest` e demais integrações também reaplicaram a V14 no boot do contexto sem erro.
- **1 erro pré-existente, fora do meu escopo**: `DashboardServiceTest.topClientesUsaJanelaDeMeses` falha por "Unresolved compilation problem" em `List<Object[]>` vs `List<Object>`. Confirmado via `git status` que `DashboardService.java`, `DashboardQueryRepository.java`, `DashboardController.java` e o novo `DashboardServiceTest` estão **modificados/não commitados por outro agente em paralelo** (task explicitamente pediu para não tocar em `DashboardService`) — não relacionado a e-mail/índices. Recomendo que o agente responsável por `DashboardService` finalize essa alteração antes da próxima rodada de `mvnw test`.

**Achados adicionais observados (fora do escopo desta tarefa, não corrigidos):**
- INFORMATIVO: `git status` mostra `security/auth/AuthService.java` e `AuthServiceTest.java` também modificados por outro agente em paralelo — não tocados por mim, mencionado aqui só para rastreabilidade caso o próximo `mvnw test` mude de resultado por causa deles.
