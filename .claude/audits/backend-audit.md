## Alteração — security/auth (refresh token reuse) + dashboard (top-clientes)

**Arquivos alterados/criados:**
- `backend/src/main/java/com/print3dmanager/erp/security/auth/AuthService.java` — `refresh()` agora distingue reuso (`revogado=true` e não expirado) de expiração simples; no reuso, chama `refreshTokenRepository.revogarTodosDoUsuario(usuarioId)` antes de lançar `BadCredentialsException` (mesma mensagem ao cliente nos dois casos).
- `backend/src/test/java/com/print3dmanager/erp/security/auth/AuthServiceTest.java` — novo teste `refreshComTokenReusadoRevogaTodasAsSessoes` (verifica chamada a `revogarTodosDoUsuario`); teste de expiração simples (`refreshRejeitaTokenExpirado`) passou a verificar `never().revogarTodosDoUsuario(any())` para garantir que expiração pura não derruba as demais sessões.
- `backend/src/main/java/com/print3dmanager/erp/dashboard/repository/DashboardQueryRepository.java` — `topClientes(int limite)` → `topClientes(Instant inicio, int limite)`, adiciona `AND p.criado_em >= :inicio` à query nativa.
- `backend/src/main/java/com/print3dmanager/erp/dashboard/service/DashboardService.java` — `topClientes(int limite)` → `topClientes(int meses, int limite)`, reaproveitando `janelaDeMeses`/`inicioDaJanela` já existentes (mesmo clamp 1–60 usado em `vendasMensais`/`consumoFilamentoMensal`); `limite` continua clampado 1–50.
- `backend/src/main/java/com/print3dmanager/erp/dashboard/controller/DashboardController.java` — `GET /dashboard/top-clientes` ganhou `@RequestParam(defaultValue = "12") int meses` (mesmo estilo `@Parameter` de `vendas-mensais`); descrição do Swagger documenta a nova janela (era "desde sempre", agora últimos 12 meses por padrão — compatível, quem chama sem o parâmetro só passa a ver dados janelados).
- `backend/src/test/java/com/print3dmanager/erp/dashboard/service/DashboardServiceTest.java` (novo) — 3 testes: janela default de 12 meses passada corretamente ao repository (via `ArgumentCaptor<Instant>`, comparando com `YearMonth.now(UTC).minusMonths(11).atDay(1)`), `meses` fora da faixa (1000) clampado a 60, `limite` fora da faixa (500) clampado a 50.

**Padrão seguido de:** o próprio módulo `dashboard` (mesmo clamp/janela de `vendasMensais`/`consumoFilamentoMensal`) e `security/auth` (mensagem de erro genérica já usada para não vazar diferença entre casos de invalidez); testes seguiram o estilo Mockito+AssertJ do `AuthServiceTest`/`ReportServiceTest` existentes (mock do `*QueryRepository` concreto).

**Build/testes:** compilou (`mvnw compile`) e suíte completa passou: `Tests run: 142, Failures: 0, Errors: 0, Skipped: 0` (`mvnw test`, JDK 21, Testcontainers/Docker ativo). Inclui `AuthServiceTest` (9 testes) e o novo `DashboardServiceTest` (3 testes).

**Achados/riscos (severidade CRÍTICO/ALTO/MÉDIO/BAIXO/INFORMATIVO):**
- INFORMATIVO: caso de borda não coberto explicitamente pelo achado — um token simultaneamente `revogado=true` E expirado (ex.: reuso detectado dias depois, já vencido) hoje **não** aciona `revogarTodosDoUsuario`, pois a regra pedida foi "revogado, especificamente não expirado". Se o objetivo for blindar também esse caso, seria só remover a condição `&& !atual.isExpirado()`. Deixei como especificado no achado; sinalizando para o agente `security` avaliar se é o comportamento desejado.

**Encaminhamentos para outros agentes:**
- `security`: revisar a decisão acima (achado #17 implementado literalmente — reuso só dispara revogação em massa quando o token ainda não expirou; considerar se reuso pós-expiração também deveria disparar).
- Nenhum outro encaminhamento — mudança de contrato do `top-clientes` é compatível (default 12 meses) e não exige ação do agente `frontend` (frontend já chama sem `meses`, mantém funcionando).
