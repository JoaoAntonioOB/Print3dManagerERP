# PROJECT_CONTEXT.md — Print3D Manager ERP

> **Propósito deste arquivo:** contexto completo do projeto para retomada do desenvolvimento em novas sessões. Leia-o integralmente antes de escrever qualquer código. Última atualização: **2026-07-18** (**Etapa 18 CONCLUÍDA** — 78 testes verdes: unitários JUnit+Mockito e integração Testcontainers+MockMvc; próxima: Etapa 19 — Melhorias finais, aguardando confirmação do usuário).

---

## 1. Objetivo do sistema

**Print3D Manager ERP** — Trabalho de Conclusão de Curso (TCC). Sistema ERP para gerenciamento completo de empresas de impressão 3D, cobrindo toda a operação: do orçamento à entrega do produto. Deve ser profissional o suficiente para uso comercial após o TCC.

**Módulos:** Dashboard, Usuários, Clientes, Pedidos, Itens do Pedido, Orçamentos, Filamentos, Estoque, Impressoras, Histórico de Impressões, Financeiro, Configurações, Relatórios.

---

## 2. Stack obrigatória

### Backend
- Java 21 · Spring Boot **3.5.6** · Maven (via **Maven Wrapper** — `mvnw`)
- Spring Security + JWT (**jjwt 0.12.5**) + Refresh Token + BCrypt
- Spring Data JPA / Hibernate · Validation
- Lombok · MapStruct **1.6.3** (com `lombok-mapstruct-binding 0.2.0`)
- Flyway (+ `flyway-database-postgresql`) · PostgreSQL 16
- springdoc-openapi **2.8.9** (Swagger UI)
- Testcontainers (testes de integração)

### Frontend (Etapa 17)
- React 19 + TypeScript + Vite
- React Router · Axios · TanStack Query · React Hook Form · Zod
- Material UI (MUI) · Recharts · React Hot Toast

### Infraestrutura
- Docker + Docker Compose + NGINX

---

## 3. Arquitetura escolhida

**Monólito modular com package-by-feature** sob `com.print3dmanager.erp`:

```
com.print3dmanager.erp/
├── config/       # OpenAPI, CORS, Web, propriedades tipadas
├── security/     # Spring Security, JWT, filtros, refresh token
├── common/       # Exceções globais, respostas padrão, paginação, base entity
├── user/  client/  printer/  filament/  inventory/
├── order/        # Pedidos, itens e histórico de impressões
├── quote/        # Orçamentos (Strategy de precificação)
├── financial/  dashboard/  report/  settings/
```

Cada módulo contém internamente: `controller/`, `service/`, `repository/`, `model/`, `dto/`, `mapper/`.

### Decisões de arquitetura já tomadas
1. **Package-by-feature** (não por camada): alta coesão por domínio, caminho aberto para extração futura de módulos.
2. **Flyway é a fonte da verdade do schema** — `ddl-auto: validate`; toda mudança de banco é uma migração versionada em `backend/src/main/resources/db/migration/`.
3. **`open-in-view: false`** — sem lazy loading na camada web.
4. **Context path do backend: `/api`** — NGINX roteia `/api` → backend:8080 e `/` → React.
5. **MapStruct global via flags do compilador** (`-Amapstruct.defaultComponentModel=spring`, `unmappedTargetPolicy=IGNORE`) — mappers injetáveis sem repetição.
6. **Perfis 12-factor**: `dev` (defaults locais: Postgres em `localhost:5432`, user/pass `print3d`/`print3d`), `prod` (tudo via env vars, sem defaults de segredos).
7. **Timezone UTC** no JDBC/Hibernate; conversão só na apresentação.
8. **Propriedades tipadas** com `@ConfigurationPropertiesScan` já habilitado na classe principal — usar `@ConfigurationProperties` (prefixo `application.*`) em vez de `@Value`.
9. **Uploads** em disco (`UPLOAD_DIR`, volume Docker `backend-uploads`), limite 100 MB/arquivo e 120 MB/request (STL/3MF).

---

## 4. Padrões e regras de código (obrigatórios)

- **Nunca simplificar o projeto.** Sempre gerar **arquivos completos** (nunca "// restante do código").
- DTOs sempre, como **Java Records**. **Nunca retornar entidades** dos controllers.
- MapStruct para entidade ↔ DTO. Paginação e filtros em todas as listagens.
- Soft delete quando fizer sentido (campo `ativo`/`deletedAt`).
- Service Layer, Repository Pattern, Strategy (precificação de orçamentos), Factory, Builder, DI.
- Transações (`@Transactional`) na camada de serviço; validações Bean Validation nos DTOs de entrada.
- Tratamento global de exceções (`@RestControllerAdvice` em `common/`).
- Swagger/OpenAPI em todos os endpoints. Métodos pequenos, código limpo, comentários só quando necessários.
- Roles: `ADMINISTRADOR`, `OPERADOR`, `FINANCEIRO`, `VISUALIZADOR`, `CLIENTE`.
- Explicar brevemente decisões de arquitetura ao entregar cada etapa.
- **Responder/documentar em português (pt-BR).**

### Entidades planejadas (resumo)
`User`, `Client`, `Filament`, `Printer`, `InventoryItem`, `Order`, `OrderItem`, `Quote` (com `shareToken` para aprovação pública), `PrintHistory`, `FinancialTransaction`, `PrinterConfiguration` (energia, valorKwh, markupPadrão, valorHoraMáquina). Detalhes completos no prompt original do TCC (campos listados por entidade).

### Regra de negócio central — cálculo de orçamento
`custo filamento + energia + hora máquina + desgaste da máquina + margem de lucro = preço sugerido` (markup editável pelo usuário; link público com `shareToken` para o cliente aprovar).

---

## 5. Ordem de implementação (aguardar confirmação do usuário entre etapas)

| # | Etapa | Status |
|---|-------|--------|
| 1 | Estrutura do projeto | ✅ Concluída |
| 2 | Docker | ✅ Concluída (testada — containers sobem, Postgres healthy) |
| 3 | Flyway | ✅ Concluída (V1–V9 aplicadas e validadas no Postgres do Docker) |
| 4 | Banco de dados (entidades JPA + BaseEntity) | ✅ Concluída (boot validado com `ddl-auto: validate`) |
| 5 | Spring Security | ✅ Concluída (401/403 JSON validados via HTTP real) |
| 6 | JWT | ✅ Concluída (login/refresh/logout testados via HTTP real) |
| 7 | Usuários | ✅ Concluída (18 cenários E2E via HTTP real) |
| 8 | Clientes | ✅ Concluída (13 cenários E2E via HTTP real) |
| 9 | Impressoras | ✅ Concluída (15 cenários E2E via HTTP real) |
| 10 | Filamentos | ✅ Concluída (20 cenários E2E via HTTP real) |
| 11 | Estoque | ✅ Concluída (21 cenários E2E via HTTP real) |
| 12 | Pedidos | ✅ Concluída (34 cenários E2E via HTTP real; upload de STL adiado) |
| 13 | Orçamentos | ✅ Concluída (30 cenários E2E via HTTP real) |
| 14 | Dashboard (gráficos + indicadores) | ✅ Concluída (17 cenários E2E via HTTP real) |
| 15 | Financeiro | ✅ Concluída (38 cenários E2E via HTTP real) |
| 16 | Relatórios (PDF) | ✅ Concluída (13 cenários E2E via HTTP real) |
| 17 | Frontend React | ✅ Concluída (13 partes — todas as telas, gráficos, relatórios e o serviço NGINX no docker-compose, tudo validado em browser real) |
| 18 | Testes (JUnit, Mockito, integração) | ✅ Concluída (78 testes: 65 unitários + 13 de integração com Testcontainers) |
| 19 | Melhorias finais (rate limit, etc.) | ⬜ |

---

## 6. O que já existe (estado real do repositório)

```
Print3d Manager ERP/
├── README.md                  # Visão geral + roadmap
├── PROJECT_CONTEXT.md         # Este arquivo
├── .gitignore  .env.example
├── docker-compose.yml         # postgres + backend ativos; frontend COMENTADO até Etapa 17
├── backend/
│   ├── pom.xml                # Todas as dependências da stack já declaradas
│   ├── Dockerfile             # Multi-stage (Maven build → JRE alpine, non-root, healthcheck)
│   ├── .dockerignore  mvnw  mvnw.cmd  .mvn/wrapper/
│   └── src/
│       ├── main/java/com/print3dmanager/erp/Print3dManagerErpApplication.java
│       ├── main/resources/application.yml        # comum + JWT/CORS/uploads (prefixo application.*)
│       ├── main/resources/application-dev.yml    # Postgres local + SQL logging
│       ├── main/resources/application-prod.yml   # tudo via env vars
│       ├── main/resources/db/migration/          # V1__..V9__ — schema base completo (pt-BR)
│       └── test/java/.../Print3dManagerErpApplicationTests.java  # teste de sanidade
└── frontend/
    ├── README.md              # placeholder — projeto Vite só na Etapa 17
    ├── Dockerfile             # Node 22 build → NGINX 1.27 (pronto, mas sem package.json ainda)
    ├── nginx.conf             # SPA + proxy /api + gzip + headers de segurança
    └── .dockerignore
```

**Build validado:** `mvnw compile` e `mvnw test` passam (Java 21 local). Aplicação bootou com perfil `dev` contra o Postgres do Docker e o Flyway aplicou as 9 migrações (`flyway_schema_history` em v9).

### Schema do banco (Etapa 3 — decisões)
- **Tabelas/colunas em português** (`usuarios`, `clientes`, `impressoras`, `configuracoes_impressora`, `filamentos`, `itens_estoque`, `pedidos`, `itens_pedido`, `orcamentos`, `historico_impressoes`, `transacoes_financeiras`); entidades Java em inglês mapeiam via `@Table`/`@Column`.
- IDs `BIGINT GENERATED ALWAYS AS IDENTITY`; timestamps `TIMESTAMPTZ` (`criado_em`/`atualizado_em`, default `now()`); soft delete via `ativo` nos cadastros mestres.
- Enums como `VARCHAR + CHECK` (não enum nativo do PG) para casar com `@Enumerated(STRING)` e facilitar evolução.
- `orcamentos.share_token UUID UNIQUE DEFAULT gen_random_uuid()` (link público de aprovação); custos decompostos (filamento/energia/hora máquina/desgaste) + `markup` em %.
- `configuracoes_impressora.impressora_id NULL` = configuração global (índice parcial único garante no máx. 1 global); 1:1 opcional com impressora.
- `numero` de pedidos/orçamentos: `VARCHAR(20) UNIQUE`, gerado pela aplicação (ex.: `PED-2026-0001`).
- `itens_pedido` deleta em cascata com o pedido; `historico_impressoes.item_pedido_id` usa `ON DELETE SET NULL`.
- Estoque de filamento fica em `filamentos` (gramas); `itens_estoque` é só para insumos gerais.

### Entidades JPA (Etapa 4 — decisões)
- **Classes em inglês, campos em português** casando com as colunas — a naming strategy padrão do Spring (camelCase → snake_case) elimina quase todos os `@Column(name=...)`; explícitos só onde a conversão falharia (ex.: `volumeXMm` → `volume_x_mm`) e nos `@JoinColumn`.
- `common/model/BaseEntity`: `@MappedSuperclass` com `id` (IDENTITY), `criadoEm`/`atualizadoEm` (`Instant` + `@CreationTimestamp`/`@UpdateTimestamp`), `equals`/`hashCode` por id.
- Todos os relacionamentos `LAZY`; `Order` tem `@OneToMany` bidirecional com `OrderItem` (cascade ALL + orphanRemoval, helpers `adicionarItem`/`removerItem`).
- `Address` é `@Embeddable` em `client/model/` (colunas achatadas em `clientes`); `estado` usa `@JdbcTypeCode(Types.CHAR)` para validar contra `CHAR(2)`.
- Enums Java espelham os CHECKs: `Role`, `PersonType`, `PrinterStatus`, `FilamentMaterial`, `OrderStatus`, `QuoteStatus`, `PrintStatus`, `TransactionType`, `TransactionStatus` — sempre `@Enumerated(STRING)`.
- `Quote.shareToken` inicializado com `UUID.randomUUID()` na aplicação (o default do banco é fallback); campos monetários `BigDecimal` com defaults `ZERO` onde o banco tem `DEFAULT 0`.
- `PrintHistory` e `PrintStatus` ficam no módulo `order` (junto de pedidos/itens, conforme arquitetura).

### Segurança (Etapa 5 — decisões)
- `security/SecurityConfig`: chain **stateless** (sem CSRF/sessão/formLogin/httpBasic), `@EnableMethodSecurity` para `@PreAuthorize` nos controllers futuros.
- Rotas públicas (sem o context path `/api`): `/auth/**` (Etapa 6), `/public/**` (aprovação de orçamento, Etapa 13), Swagger (`/v3/api-docs/**`, `/swagger-ui/**`), `/actuator/health|info`, e `OPTIONS /**` (preflight CORS). Todo o resto exige autenticação.
- 401/403 respondem JSON via `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` usando `common/dto/ApiErrorResponse` (record: timestamp, status, error, message, path) — mesmo formato que o futuro `@RestControllerAdvice` usará.
- `DatabaseUserDetailsService` carrega por e-mail via `UserRepository` (primeiro repository do projeto); `SecurityUser` adapta a entidade `User` (authority `ROLE_<role>`, `isEnabled` ← `ativo`).
- `PasswordEncoder` BCrypt e `AuthenticationManager` (via `AuthenticationConfiguration`) já expostos como beans para o login JWT da Etapa 6.
- CORS centralizado em `CorsConfigurationSource` lendo `config/CorsProperties` (`application.cors.allowed-origins`), com `allowCredentials` e header `Content-Disposition` exposto (downloads de relatórios).

### JWT (Etapa 6 — decisões)
- **Access token JWT** (jjwt 0.12.5, HMAC — chave Base64 de `application.security.jwt.secret` via `config/JwtProperties`): subject = e-mail, claims `uid` e `role`, 15 min. **Refresh token opaco** (UUID) persistido em `refresh_tokens` (migração V10), 7 dias, **com rotação**: cada uso revoga o token e emite novo par; reuso → 401. Multissessão permitida.
- `security/jwt/JwtAuthenticationFilter` (antes do `UsernamePasswordAuthenticationFilter`): token inválido apenas segue sem autenticação (401 vem da autorização); usuário é **recarregado do banco** a cada requisição (desativação tem efeito imediato).
- `security/auth/`: `AuthController` (`POST /auth/login|refresh|logout`), `AuthService`, entidade `RefreshToken`, repository e DTOs record com Bean Validation. Resposta de login: campos de token em convenção OAuth (`accessToken`, `refreshToken`, `tokenType`, `expiresIn` em segundos) + objeto `usuario` em pt-BR.
- **Tratamento global de exceções** criado em `common/exception/`: `GlobalExceptionHandler` (@RestControllerAdvice) cobre validação (400 com lista `errors` por campo), credenciais/token inválidos (401 — mensagem genérica no login para não revelar e-mails existentes), 403, 404 (`ResourceNotFoundException` + rota inexistente), `BusinessException` (400) e 500 genérico com log. `ApiErrorResponse` ganhou campo opcional `errors`.
- `config/OpenApiConfig`: título/descrição da API + esquema `bearerAuth` global (botão Authorize no Swagger); `/auth/**` anotado com `@SecurityRequirements` (sem cadeado).
- **Migração V11**: usuário admin inicial `admin@print3d.com` / `admin123` (BCrypt custo 10) — **trocar senha em produção**.

### Módulo Usuários (Etapa 7 — PADRÃO PARA OS DEMAIS CRUDs)
O módulo `user/` define o padrão que TODOS os próximos módulos devem seguir:
- **Estrutura**: `controller/` + `service/` + `repository/` (com classe `*Specifications`) + `dto/` (records com Bean Validation e `@Schema`) + `mapper/` (MapStruct: `toResponse`, `toEntity`, `atualizar(@MappingTarget ...)` — senha/campos sensíveis ignorados no mapper e tratados no service).
- **Listagem**: `PageResponse<T>` (`common/dto/`, envelope estável: content/page/size/totalElements/totalPages/first/last) + `JpaSpecificationExecutor` com filtros opcionais (parâmetro nulo = ignorado) + `@ParameterObject @PageableDefault(sort=...) Pageable`.
- **Conflitos**: `ResourceConflictException` → 409 (e-mail duplicado etc.); sort inválido (`PropertyReferenceException`) → 400; ambos no handler global.
- **Autorização**: `@PreAuthorize("hasRole('ADMINISTRADOR')")` método a método (explícito, sem herança de classe); endpoints `/users/me` e `/users/me/senha` para qualquer autenticado via `@AuthenticationPrincipal SecurityUser`.
- **Regras**: soft delete (`DELETE` → `ativo=false` + revoga refresh tokens; login passa a dar 401 na hora); não pode desativar a si mesmo (400); `PATCH /{id}/ativar` reativa; troca de senha exige senha atual e revoga todas as sessões do usuário.
- Endpoints: `GET /users` (busca nome/e-mail, role, ativo, paginado), `GET/PUT/DELETE /users/{id}`, `POST /users`, `PATCH /users/{id}/ativar`, `GET /users/me`, `PATCH /users/me/senha`.

### Módulo Clientes (Etapa 8)
- Segue o padrão do módulo Usuários. Rotas `/clients` (inglês, como `/users`). Dois níveis de acesso via constantes `PODE_GERENCIAR` (ADMINISTRADOR, OPERADOR) e `PODE_CONSULTAR` (+ FINANCEIRO, VISUALIZADOR — CLIENTE não acessa).
- `AddressDto` aninhado com validações de UF (`[A-Z]{2}`) e CEP (`\d{5}-?\d{3}`); MapStruct mapeia o embeddable automaticamente; PUT substitui o endereço por completo (inclusive para null).
- CPF/CNPJ: opcional, armazenado como informado (com máscara); em branco vira `null` (unicidade só vale quando informado) → duplicado dá 409. Filtros: `busca` (nome/e-mail/CPF-CNPJ), `tipoPessoa`, `ativo`.
- Soft delete preserva histórico de pedidos/orçamentos (FKs apontam para o cliente).

### Módulo Impressoras (Etapa 9)
- CRUD padrão em `/printers` (filtros: busca nome/marca/modelo, status, ativo). `PATCH /{id}/status` muda a situação operacional (recusa em impressora desativada → 400); soft delete marca INATIVA, reativação volta DISPONIVEL.
- **Configurações de custo** (`PrinterConfigurationController`, escrita **só ADMINISTRADOR**): `GET|PUT /printers/config` (global, upsert) e `GET|PUT|DELETE /printers/{id}/config` (própria, upsert). O `GET /{id}/config` retorna a **configuração efetiva** — própria se existir, senão global — com campo `origem` (`PROPRIA`/`GLOBAL`); sem global cadastrada → 404 com mensagem orientando o PUT. **A Etapa 13 (Orçamentos) deve usar `PrinterConfigurationService.buscarEfetiva`** como fonte dos parâmetros de custo.

### Módulo Filamentos (Etapa 10)
- CRUD padrão em `/filaments` (filtros: busca nome/marca/cor, `material`, `ativo`, `estoqueBaixo`). Mesmos níveis de acesso das impressoras (`PODE_GERENCIAR` / `PODE_CONSULTAR`).
- `FilamentResponse` traz o campo calculado **`estoqueBaixo`** (`quantidade ≤ estoque mínimo`) — método `Filament.isEstoqueBaixo()` na entidade, mapeado automaticamente pelo MapStruct; o filtro `estoqueBaixo` da listagem compara as duas colunas na Specification.
- **Estoque só muda por `PATCH /filaments/{id}/estoque`** (`{tipo: ENTRADA|SAIDA, quantidadeG}` — enum `StockMovementType`, em `common/model/` desde a Etapa 11): saída maior que o saldo → 400; movimentação em filamento desativado → 400. O PUT **não** toca em `quantidadeEstoqueG` (campo ausente do `FilamentUpdateRequest`).
- Defaults do cadastro via MapStruct `defaultValue` no `toEntity` (diâmetro `1.75`, estoques `0`) — campos omitidos no POST assumem os mesmos defaults do banco.
- **A Etapa 12 (Pedidos) fará o consumo automático de estoque** ao registrar impressões; a movimentação manual desta etapa cobre reposição/ajustes/perdas.

### Módulo Estoque (Etapa 11)
- Espelho do módulo Filamentos para insumos gerais (`InventoryItem`, tabela `itens_estoque` da V5 — filamento NÃO entra aqui). Rotas `/inventory`, mesmos níveis de acesso (`PODE_GERENCIAR` / `PODE_CONSULTAR`).
- Filtros: `busca` (nome/descrição/categoria/localização), `categoria` (igualdade case-insensitive), `ativo`, `estoqueBaixo` (quantidade ≤ quantidade mínima, com `isEstoqueBaixo()` na entidade + comparação de colunas na Specification).
- **Quantidade só muda por `PATCH /inventory/{id}/estoque`** (`{tipo: ENTRADA|SAIDA, quantidade}` na unidade de medida do item, `NUMERIC(12,3)` aceita fração): saldo negativo → 400 (mensagem inclui a `unidadeMedida`); item desativado → 400. PUT não toca em `quantidade`.
- Defaults do cadastro via MapStruct `defaultValue`: quantidades `0`, `unidadeMedida` `UN`.
- **`StockMovementType` foi promovido para `common/model/`** (compartilhado entre `/filaments` e `/inventory`).

### Módulo Pedidos + Impressões (Etapa 12)
- **`/orders`**: listagem retorna `OrderSummaryResponse` (sem itens, `@EntityGraph` no cliente evita N+1); `GET /{id}` retorna `OrderResponse` completo (`findDetalhadoById` com grafo cliente/usuário/itens/filamento). Filtros: `busca` (número/nome do cliente), `status`, `clienteId`; sort default `criadoEm,desc`.
- **Número do pedido**: `PED-<ano>-<seq 4 dígitos>` gerado em `OrderService.gerarNumero()` — `pg_advisory_xact_lock(hashtext('pedidos_numero_<ano>'))` serializa a geração concorrente (backstop: unique constraint). Último número do ano localizado por `findTopByNumeroStartingWithOrderByIdDesc` (id, não string — sobrevive a sequências > 9999).
- **Itens**: geridos junto com o pedido. `PUT /orders/{id}` substitui a lista (com `id` atualiza, sem `id` cria, ausentes removidos via orphanRemoval; item de outro pedido → 400). `valorTotal` sempre recalculado (Σ `quantidade × precoUnitario` − `desconto`; desconto > subtotal → 400). `OrderItem.getSubtotal()` exposto nos DTOs. `arquivoModelo` fica intacto no PUT (upload de STL adiado — não está nos DTOs de escrita).
- **Máquina de estados** (`PATCH /{id}/status`): PENDENTE→EM_PRODUCAO|CANCELADO, EM_PRODUCAO→CONCLUIDO|CANCELADO, CONCLUIDO→ENTREGUE (grava `dataEntregaRealizada`); demais → 400. Edição (PUT) só em PENDENTE; `DELETE` (hard, cascata) só em PENDENTE — fora disso, cancelar.
- **`/prints` (PrintHistory)**: `POST` inicia job (impressora precisa estar DISPONIVEL e ativa → vira IMPRIMINDO — bloqueia 2 jobs simultâneos na mesma máquina; item de pedido, se informado, exige pedido EM_PRODUCAO; operador = usuário autenticado). `PATCH /{id}/concluir|falhar|cancelar` (só de EM_ANDAMENTO): libera a impressora, soma `horasImpressaoTotal`, calcula `tempoTotalMinutos` e **abate `pesoUtilizadoG` do estoque do filamento** (também em falhas — material desperdiçado; saldo insuficiente → 400 e a transação reverte). `falhar` exige `motivoFalha`; `cancelar` aceita corpo opcional.
- **Custo real do job** (`custoTotal`): filamento (peso × custo/kg) + energia (kWh × `valorKwh`) + máquina ((`valorHoraMaquina` + `custoDesgasteHora`) × horas), usando `PrinterConfigurationService.buscarEfetivaOpcional` (novo método — efetiva sem exigir global cadastrada; componentes sem dados são omitidos, sem nenhum → null).
- Filtros de `/prints`: `impressoraId`, `status`, `itemPedidoId`, `de`/`ate` (ISO-8601 sobre `iniciadoEm`); sort default `iniciadoEm,desc`; respostas com nomes resolvidos (impressora/filamento/peça/número do pedido/operador) via `@EntityGraph`.

### Módulo Orçamentos (Etapa 13)
- **Strategy de precificação** em `quote/service/pricing/`: interface `PricingStrategy` (`calcular(PricingInput) → PricingResult`) com a política padrão `CostMarkupPricingStrategy` — filamento (peso × custo/kg) + energia (potência da impressora × horas × `valorKwh`) + hora máquina + desgaste, e markup % sobre o total. Componentes sem dados entram como zero; **cada componente é arredondado antes da soma** para a conta exibida fechar com o preço. Novas políticas (ex.: preço por grama) plugam pela interface sem tocar no `QuoteService`.
- **`/quotes`**: CRUD no padrão (filtros `busca` número/cliente, `status`, `clienteId`); número `ORC-<ano>-<seq>` (mesmo advisory lock dos pedidos, chave própria); `markup` omitido no POST usa o `markupPadrao` da configuração efetiva (`buscarEfetivaOpcional(impressoraId nullable)` — sem impressora cai na global; fallback final 100%); `precoFinal` opcional sobrepõe o sugerido (`precoEfetivo` = final ?? sugerido, exposto como getter da entidade junto com `custoTotal`).
- **Ciclo**: RASCUNHO→ENVIADO; ENVIADO→RASCUNHO (volta para editar)|APROVADO|REJEITADO|EXPIRADO; APROVADO/REJEITADO/EXPIRADO/CONVERTIDO terminais via PATCH. Edição (PUT, recalcula custos) e DELETE só em RASCUNHO. `CONVERTIDO` **apenas** via `POST /quotes/{id}/converter`.
- **Link público** (`/public/quotes/{shareToken}`, sem login): `GET` (visão do cliente — `PublicQuoteResponse` SEM custos internos/markup/preço sugerido, só o preço efetivo), `POST .../aprovar|recusar` (exigem ENVIADO). RASCUNHO → 404; token inválido → 404 (parse de UUID no service). **Expiração preguiçosa**: ENVIADO com `dataValidade` vencida vira EXPIRADO em qualquer acesso público (métodos públicos são `@Transactional` de escrita por isso).
- **Conversão em pedido**: exige APROVADO; monta `OrderCreateRequest` (item único: nomePeca = descrição truncada em 160 ou "Orçamento NUM", preço unitário = `precoEfetivo`, peso/tempo/filamento do orçamento) e **reusa `OrderService.criar`** (número PED, validações); vincula `pedido_id`, marca CONVERTIDO e retorna o `OrderResponse` (201).

### Módulo Dashboard (Etapa 14)
- Somente leitura, acessível a todos os perfis internos (`PODE_CONSULTAR`); **parâmetros fora da faixa são ajustados** (`Math.clamp`), não rejeitados. Sem entidade/migração nova.
- `DashboardQueryRepository` (`@Repository` com `EntityManager`, SQL nativo): concentra as agregações para o módulo ficar autocontido em vez de espalhar `@Query` pelos repositories dos outros módulos. Meses agrupados com `to_char(..., 'YYYY-MM')` em UTC.
- **Endpoints**: `GET /dashboard/resumo` (pedidos/orçamentos/impressoras por status — **todas as chaves do enum presentes, ausência = 0** —, filamentos e itens com estoque baixo, clientes ativos, impressões em andamento, pedidos e faturamento do mês corrente); `vendas-mensais?meses=` (pedidos abertos por `criado_em` + faturamento ENTREGUE por `data_entrega_realizada`); `consumo-filamento?meses=` (Σ `peso_utilizado_g` por mês de `finalizado_em`); `taxa-sucesso` (CONCLUIDA ÷ (CONCLUIDA+FALHOU) %, null sem finalizadas); `top-clientes?limite=` (Σ `valor_total` sem CANCELADOs, ordem decrescente).
- **Séries mensais sempre devolvem os N meses completos** (janela 1–60 incluindo o corrente, faltantes zerados, ordem cronológica) — prontas para os gráficos Recharts da Etapa 17 sem tratamento no frontend.

### Módulo Financeiro (Etapa 15)
- **Perfis diferentes do padrão dos CRUDs**: ADMINISTRADOR e **FINANCEIRO** gerenciam (`PODE_GERENCIAR`); OPERADOR e VISUALIZADOR apenas consultam. Sem migração nova (tabela `transacoes_financeiras` da V9; `valor` sempre positivo — o sinal é dado por `tipo` RECEITA/DESPESA).
- **`/financial/transactions`**: CRUD no padrão (filtros: `busca` descrição/categoria/observações, `tipo`, `status`, `categoria` exata case-insensitive, `pedidoId`, `clienteId`, `de`/`ate` sobre `dataTransacao`; sort default `dataTransacao,desc`; `@EntityGraph` pedido/cliente evita N+1). Vínculos opcionais a pedido e cliente; **cliente omitido com pedido informado herda o cliente do pedido** (POST e PUT).
- **Ciclo de vida** (`PATCH /{id}/status`): PENDENTE→PAGA|CANCELADA, PAGA→PENDENTE (estorno da baixa); CANCELADA é terminal; demais → 400. Cadastro com status CANCELADA → 400 (status omitido assume PENDENTE; o mapper ignora `status` e o service aplica). **PUT e DELETE só em PENDENTE** — paga/cancelada preserva histórico (cancelar em vez de excluir); DELETE é físico.
- **Faturamento de pedidos** (`financial/service/OrderBillingService`): `POST /orders/{id}/faturar` (só ADMINISTRADOR/FINANCEIRO) exige pedido CONCLUIDO ou ENTREGUE e cria receita PENDENTE (categoria "Vendas", valor = `valorTotal`, data = entrega realizada ?? hoje, vínculos pedido+cliente); pedido já com receita não cancelada → **409**; pedido sem valor → 400. **Entrega automática**: `OrderService.alterarStatus(ENTREGUE)` chama `gerarReceitaSeNecessario` — silencioso se já faturado ou sem valor (receita CANCELADA libera refaturamento — checagem `existsByPedidoIdAndTipoAndStatusNot`).
- **Resumos** (`FinancialQueryRepository`, SQL nativo no padrão do dashboard): `GET /financial/resumo?de&ate` (default mês corrente UTC; CANCELADAS fora) → receitas/despesas pagas e pendentes, `saldoRealizado` (pagas) e `saldoPrevisto` (pagas+pendentes); `de` > `ate` → 400. `GET /financial/resumo/mensal?meses=` (1–60, `Math.clamp`) → série só de transações **PAGAS**, N meses completos zerados em ordem cronológica (padrão das séries do dashboard).

### Módulo Relatórios (Etapa 16)
- **OpenPDF 2.2.2** (`com.github.librepdf:openpdf`, decidido com o usuário — fork livre do iText 4, layout em código, sem templates externos). Sem entidade/migração nova.
- **`report/service/pdf/PdfReportBuilder`** (padrão Builder): concentra TODO o layout OpenPDF — A4, cabeçalho (nome do sistema + título + período + gerado em UTC), tabela zebrada com colunas numéricas à direita, bloco de totais rótulo→valor, rodapé com numeração; período vazio vira "Nenhum registro no período." Os services só descrevem conteúdo (`comPeriodo`/`comColunas`/`comLinha`/`comTotal`/`gerar`). Fontes Helvetica built-in (WinAnsi cobre pt-BR).
- **Endpoints** (GET, `produces application/pdf`, `Content-Disposition: attachment` com nome `relatorio-<tipo>_<de>-<ate>.pdf`; período default mês corrente UTC; `de` > `ate` → 400): `/reports/pedidos?de&ate&status` (pedidos abertos no período por `criado_em`, filtro opcional de status; totais: quantidade, cancelados, valor exceto cancelados); `/reports/financeiro?de&ate` (transações por `data_transacao`, canceladas identificadas + resumo reaproveitando `FinancialTransactionService.resumo`) — **restrito a ADMINISTRADOR/FINANCEIRO** (decidido com o usuário); `/reports/consumo-filamento?de&ate` (agregado por filamento das impressões finalizadas com peso registrado; totais de jobs/gramas/custo).
- `ReportQueryRepository` com SQL nativo (padrão do dashboard); datas formatadas no próprio SQL (`to_char DD/MM/YYYY`) para não depender do mapeamento de tipos do Hibernate em query nativa; moeda formatada em pt-BR no service.
- **Handler global ganhou `MethodArgumentTypeMismatchException` → 400** (enum/tipo inválido em query param dava 500 em TODOS os módulos; mensagem lista os valores aceitos quando o alvo é enum).

### Frontend React (Etapa 17 — progresso)

**Concluídas e commitadas (validadas de ponta a ponta em browser real — scripts playwright-core + Edge headless):**
- **Parte 2 — Clientes** (`pages/clients/`): o CRUD **modelo** dos demais — toolbar de filtros (busca com `useDebounce`, selects nativos), tabela paginada server-side (`TablePagination`), form em Dialog (RHF+zod, grid responsivo), soft delete com `ConfirmDialog` + reativação, botões de escrita escondidos de quem não gerencia (`podeGerenciar` por role). Componentes compartilhados criados: `components/ConfirmDialog`, `hooks/useDebounce`.
- **Parte 3 — Filamentos** (`pages/filaments/`): + diálogo de movimentação de estoque em gramas; destaque visual de estoque baixo; filtro "só estoque baixo"; **`lib/form.ts`** criado (campos numéricos como texto pt-BR — aceita vírgula —, helpers `decimalObrigatorio/decimalOpcional/inteiroOpcional/paraNumero/paraCampo/paraTexto`). Estoque inicial só no cadastro (PUT não envia quantidade).
- **Parte 4 — Estoque de insumos** (`pages/inventory/`): espelho de Filamentos na unidade de medida do item.
- **Parte 5 — Impressoras** (`pages/printers/`): CRUD + diálogo de status operacional + **configurações de custo** (`PrinterConfigDialog` serve global e por-impressora: mostra origem PRÓPRIA/“Herdada da global”, salvar própria, remover própria; escrita só ADMINISTRADOR — refletido na UI).

- **Parte 6 — Pedidos** (`pages/orders/`): `api/orders.ts` (tipos + `TRANSICOES_PEDIDO` espelhando a máquina de estados do backend), `OrdersPage.tsx` (listagem com ações: ver/editar, status, **faturar** com confirmação — só ADMIN/FINANCEIRO e CONCLUIDO/ENTREGUE —, excluir só PENDENTE), `OrderStatusDialog.tsx` (só transições válidas), `OrderFormDialog.tsx` (cliente via Autocomplete com busca remota, itens com `useFieldArray`, total calculado ao vivo; vira **somente leitura** quando status ≠ PENDENTE). **Gotcha zod+RHF resolvido**: `.refine` no `clienteId` faz input≠output no zod — tipar `useForm<z.input<...>, unknown, z.output<...>>` e o handler/`paraPayload` recebem o output (clienteId já `number`). **Gotcha playwright+MUI**: o Tooltip clona `aria-label` para o span wrapper — asserts de `disabled` precisam do prefixo `button[aria-label=...]`; e o Drawer do menu também é `role=dialog` (não esperar "dialog detached" genérico). Validado ponta a ponta (`drive-pedidos.mjs`): criar (validação zod, 2º item removido, total ao vivo), editar, PENDENTE→EM_PRODUCAO→CONCLUIDO→faturar→ENTREGUE, leitura em ENTREGUE, excluir PENDENTE, filtro por status.

- **Parte 7 — Orçamentos** (`pages/quotes/`): `api/quotes.ts` (tipos + `TRANSICOES_ORCAMENTO`), `QuotesPage.tsx` (ações: ver/editar, copiar link público — desabilitado em RASCUNHO —, status, **converter em pedido** só APROVADO, excluir só RASCUNHO), `QuoteStatusDialog.tsx`, `QuoteFormDialog.tsx` (cliente Autocomplete, impressora/filamento selects, quadro "Custos calculados" na edição/leitura; edição só em RASCUNHO; markup omitido herda da config efetiva). **`PublicQuotePage.tsx` em rota pública `/orcamento/:shareToken`** (fora do RequireAuth, mesmo visual do login): visão do cliente com preço, Aprovar/Recusar quando ENVIADO, mensagens por status final. **Dois bugs reais achados na validação**: (1) derivar o objeto do Autocomplete da query de clientes faz o value oscilar para null durante refetch → loop infinito de render do MUI ("Maximum update depth") — corrigido guardando o selecionado em estado local + `placeholderData` (aplicado também no OrderFormDialog); (2) **o Jackson usa `default-property-inclusion: non_null`** — campos nulos são OMITIDOS do JSON e chegam como `undefined` no frontend: nunca comparar dados da API com `!== null` estrito, usar `!= null` (dava "R$ NaN" no preço final). Validado ponta a ponta (`drive-orcamentos.mjs`): criar (markup herdado da config global = 120), editar, enviar, copiar link, aprovar no link público, converter em pedido, leitura do convertido, recusar via link, excluir rascunho, filtro.

- **Parte 8 — Impressões** (`pages/prints/`): `api/prints.ts`, `PrintsPage.tsx` (filtros impressora+status; ações só em EM_ANDAMENTO: concluir/falhar/cancelar), `PrintStartDialog.tsx` (impressora só DISPONIVEL+ativa; pedido EM_PRODUCAO opcional → select dependente de item via `GET /orders/{id}`; filamento opcional), `PrintFinishDialog.tsx` (modo concluir|falhar — falha exige motivo; peso abate estoque do filamento nos dois casos), cancelar via ConfirmDialog (PATCH sem corpo). Invalida queries de `prints`+`printers` (+`filaments` na finalização) para refletir status da máquina e estoque. **Gotcha de teste**: checagem de filtro em lista com `placeholderData` deve esperar as linhas filtradas SUMIREM (state detached) — esperar a linha presente passa com dados stale. Validado (`drive-impressoes.mjs`): job vinculado a item de pedido, impressora ocupada some do select de novo job, concluir com peso/energia (custo real calculado), falhar com motivo, cancelar liberando a máquina, filtros.

- **Parte 9 — Financeiro** (`pages/financial/`): `api/financial.ts` (tipos + `TRANSICOES_TRANSACAO`), `FinancialPage.tsx` (**cards do resumo do mês corrente** via `/financial/resumo` acima dos filtros; tabela com tipo/vínculo/valor com sinal; **`podeGerenciar` = ADMINISTRADOR/FINANCEIRO**, diferente dos CRUDs), `TransactionFormDialog.tsx` (status inicial PENDENTE|PAGA só no cadastro; **vínculos pedido/cliente são preservados na edição** — o form repassa os ids da transação sem exibi-los), `TransactionStatusDialog.tsx` (baixa/estorno/cancelamento). Editar/excluir só PENDENTE; cancelada bloqueia tudo. Invalidação por prefixo `['financial']` atualiza lista + resumo juntos. Validado (`drive-financeiro.mjs`): resumo reage a lançamento (comparação antes/depois do card — valores acumulam entre execuções, não usar valor exato), despesa paga, receita pendente→baixa→estorno→edição→cancelamento, exclusão, filtros com espera de linha sumir.

- **Parte 10 — Usuários** (`pages/users/`): `api/users.ts` (+`ROTULOS_ROLE`), `UsersPage.tsx` (rota já restrita a ADMINISTRADOR pelo menu/perfis; filtros busca/papel/situação; desativar próprio usuário bloqueado na UI — backend também recusa; desativar revoga sessões, reativar via botão), `UserFormDialog.tsx` (senha só no cadastro). **`components/ChangePasswordDialog.tsx` pendurado no `AppLayout`** (ícone de chave no AppBar, disponível a todos os perfis): confirmação client-side, e após trocar chama `logout()` — o backend revoga todas as sessões, então o fluxo leva ao login. Validado (`drive-usuarios.mjs`): CRUD, 409 de e-mail duplicado, desativar/reativar, filtro por papel, sessão de OPERADOR sem menu Usuários, troca de senha com confirmação errada → certa → relogin (antiga falha, nova entra).

- **Parte 11 — Gráficos do dashboard** (`api/dashboard.ts` + `DashboardPage.tsx` reescrita): mantém os 4 cards e adiciona 5 gráficos Recharts + 1 stat tile — faturamento mensal (barras), pedidos abertos por mês (linha), receitas × despesas pagas (barras agrupadas com legenda), consumo de filamento (barras), top 5 clientes (barras horizontais), taxa de sucesso como número-manchete com chips (um valor único não é gráfico). **Paleta validada com a skill dataviz** (`validate_palette.js`): azul `#3572b0` (séries únicas/receitas) + laranja `#c8611a` (despesas) — o azul-petróleo do tema (#34495e) REPROVA como cor de dado (escuro/acinzentado demais). Regras seguidas: nunca eixo duplo (pedidos e faturamento viraram 2 gráficos), barras finas com topo arredondado, grid recessivo, tooltips nativos. Gotcha Recharts v3+TS: `labelFormatter` recebe `ReactNode` — envolver com `String()`.

- **Parte 12 — Relatórios** (`pages/reports/ReportsPage.tsx` + `api/reports.ts`): período compartilhado (default mês corrente) + 3 cards de download; card Financeiro só aparece para ADMINISTRADOR/FINANCEIRO; relatório de pedidos tem filtro opcional de status; `de > ate` barrado no cliente com toast. Download via axios `responseType: 'blob'` + `<a download>` com nome extraído do `Content-Disposition` (o CORS do backend já expõe o header). `PlaceholderPage` removida — todas as rotas têm tela real.

- **Parte 13 — Docker/NGINX (fim da etapa)**: serviço `frontend` descomentado no `docker-compose.yml` (build multi-stage Node 22 → NGINX 1.27, porta 80). Validado em browser real contra `http://localhost`: SPA servida, login via proxy same-origin `/api`, dashboard com gráficos, F5 em rota profunda (fallback `try_files`), rota pública `/orcamento/:token`. **A stack completa sobe com `docker compose up -d`.**

### Frontend React (Etapa 17 — parte 1)
- Projeto Vite (react-ts) criado em `frontend/` preservando Dockerfile/nginx.conf; estrutura em `src/`: `api/` (Axios + tipos), `auth/` (AuthProvider + RequireAuth + localStorage), `layout/` (AppLayout), `pages/`, `theme.ts` (azul-petróleo #34495e, mesmo dos PDFs; locale ptBR). Detalhes de uso no `frontend/README.md`.
- **Auth**: tokens em localStorage; interceptor Axios anexa Bearer e, em 401 (fora de `/auth/*`), renova via `/auth/refresh` com **uma promise compartilhada** (concorrentes aguardam a mesma renovação) e reexecuta a chamada; refresh falhou → limpa sessão, evento `print3d:sessao-expirada`, toast e volta ao login. Logout chama `POST /auth/logout` com o refresh token.
- **Rotas**: `/login` pública; demais dentro de `<RequireAuth><AppLayout/></RequireAuth>` (redirect guarda o destino em `state.de`). Menu lateral filtrado por `role` (Usuários só ADMINISTRADOR); páginas placeholder para os módulos ainda sem tela; Dashboard inicial com cards de `/dashboard/resumo` via TanStack Query.
- **Proxy dev** (`vite.config.ts`): `/api` → `localhost:8080` **removendo o header Origin** — sem isso o backend do Docker (CORS `http://localhost`) responde 403 "Invalid CORS request" ao Origin `http://localhost:5173`. Em produção o NGINX já faz proxy same-origin.
- **Gotchas da stack 2026**: MUI **v9** não aceita system props direto no componente (`fontWeight`, `alignItems`, `flexWrap` → sempre via `sx`); zod **v4** usa `z.email()` (não `z.string().email()`); scaffold Vite usa oxlint (`npm run lint`).
- **Validação**: `npm run build` (tsc + vite) e lint verdes; fluxo completo dirigido em browser real (playwright-core + Edge headless, script no scratchpad): login → erro de credencial → dashboard com dados → navegação → F5 mantém sessão → logout.
- **Pendente nas próximas partes**: telas CRUD dos módulos (pedidos, orçamentos, clientes, filamentos, estoque, impressoras, impressões, financeiro, usuários), gráficos Recharts no dashboard, downloads dos relatórios PDF, e descomentar o serviço `frontend` no docker-compose ao final.

### Testes (Etapa 18 — decisões)
- **Duas camadas** em `backend/src/test/java`:
  - **Unitários (JUnit 5 + Mockito, 65 testes)** — services com regra de negócio, repositories/mappers mockados: `CostMarkupPricingStrategyTest` (a conta central: componentes arredondados antes da soma fecham com o preço; componentes sem dados zeram), `OrderServiceTest` (número sequencial por ano, total Σ itens − desconto, máquina de estados com terminais, mescla de itens do PUT, exclusão só PENDENTE), `PrintHistoryServiceTest` (ocupação/liberação da impressora, soma de horas, abate de estoque também em falha, custo real, estoque insuficiente reverte), `OrderBillingServiceTest` (409 de refaturamento, entrega automática silenciosa), `PrinterConfigurationServiceTest` (efetiva: própria > global > vazio), `AuthServiceTest` (rotação de refresh, replay/expirado/inativo rejeitados, logout idempotente) e `JwtServiceTest` (roundtrip, chave alheia, expirado, lixo — sempre `Optional.empty`, nunca exceção). Stubs dos mappers MapStruct reproduzem os `defaultValue` (ex.: quantidade 1).
  - **Integração (Testcontainers + MockMvc, 13 testes)** — `testsupport/AbstractIntegrationTest`: **singleton container** PostgreSQL 16-alpine com `@ServiceConnection` e perfil `test` (`src/test/resources/application-test.yml` só abaixa o log); um único contexto Spring cacheado atende todas as classes. `Print3dManagerErpApplicationTests` valida boot + 11 migrações Flyway + admin da V11. `AbstractApiIntegrationTest` (`@AutoConfigureMockMvc`) dá `loginAdmin()`/`json()`/`bearer()` — **MockMvc não usa o context path `/api`**, as rotas são as dos controllers. Fluxos: `AuthFlowIntegrationTest` (login, rotação + replay 401, logout, 401/403 em JSON), `OrderFlowIntegrationTest` (criar→produzir→concluir→faturar 201/409→entregar sem receita duplicada; transição inválida 400; DELETE fora de PENDENTE 400), `QuoteFlowIntegrationTest` (markup herdado da config global, RASCUNHO invisível no link público, visão pública sem custos internos, aprovar/recusar, conversão 201 e recusa bloqueia conversão). Testes criam seus próprios dados com e-mails únicos (`System.nanoTime()`) — o banco é compartilhado pela suíte.
- **Gotcha Docker 29**: o engine 29+ recusa a versão de API antiga do docker-java do Testcontainers 1.21 (npipe respondia 400 e o Testcontainers "não achava" o Docker). Resolvido fixando `api.version=1.44` via `systemPropertyVariables` do Surefire no `pom.xml` — `mvnw test` funciona sem flags.
- `verify(repo, never()).delete(any())` é ambíguo em repositories com `JpaSpecificationExecutor` (Spring Data 3.5 tem `delete(Specification)`) — usar `delete(any(Entidade.class))`.

### Configurações-chave já definidas (application.yml)
- `application.security.jwt.secret|access-token-expiration|refresh-token-expiration` (access 15 min, refresh 7 dias)
- `application.cors.allowed-origins` (dev: `http://localhost:5173`)
- `application.storage.upload-dir`
- Swagger UI: `/api/swagger-ui.html` · Actuator: `health,info,metrics`

---

## 7. Pendências e observações do ambiente

1. **Docker Desktop instalado e funcionando** — `docker compose up -d postgres` sobe o banco (container `print3d-postgres`, healthy). O projeto também já está no GitHub.
2. **`JAVA_HOME` não está configurado** no Windows. O JDK está em `C:\Program Files\Java\jdk-21.0.10` — setar na sessão antes de rodar o `mvnw` (`$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'`). Recomendar configurar como variável de sistema.
3. **Maven não está instalado** — usar sempre `.\mvnw.cmd` (Windows) dentro de `backend/`.
4. A stack completa (postgres + backend + frontend/NGINX) sobe com `docker compose up -d`; o app fica em `http://localhost` e o Swagger em `http://localhost/api/swagger-ui.html` (ou `:8080` direto).
5. O git repo root é a **própria pasta do projeto** (`C:\repository\Print3d Manager ERP`), com remote `origin` → `github.com/JoaoAntonioOB/Print3dManagerERP`.
6. Segredo JWT tem **default de dev** no yml/compose (base64) — em produção deve vir do `.env` (`openssl rand -base64 48`).
7. **`mvnw test` roda a suíte completa (78 testes)** — os de integração exigem o Docker Desktop em execução (Testcontainers baixa `postgres:16-alpine` e `testcontainers/ryuk` na primeira vez). A pinagem `api.version=1.44` no Surefire cobre o Docker Engine 29+.
8. Máquina: Windows 11, PowerShell 5.1. O terminal do usuário usa pt-BR.

---

## 8. Como retomar o desenvolvimento

1. Ler este arquivo e o `README.md`.
2. Confirmar o status da tabela da seção 5 com o usuário.
3. A próxima etapa é a **19 — Melhorias finais (rate limit etc.)** — **aguardar confirmação do usuário antes de começar**. Candidatas discutidas no prompt original: rate limiting, troca da senha default do admin, upload de STL/3MF (adiado desde a Etapa 12), revisão de segurança para produção. Regressões: `mvnw test` no backend (78 testes) e o fluxo de validação em browser real (scripts em `scratchpad/browser-drive/`, playwright-core + canal msedge) para o frontend. Toda a API está no Swagger (`/api/swagger-ui.html`).
4. Ao final de cada etapa: explicar decisões, validar build (`.\mvnw.cmd -B compile`) e **aguardar confirmação do usuário** antes da próxima etapa.
