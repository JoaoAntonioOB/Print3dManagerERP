# PROJECT_CONTEXT.md — Print3D Manager ERP

> **Propósito deste arquivo:** contexto completo do projeto para retomada do desenvolvimento em novas sessões. Leia-o integralmente antes de escrever qualquer código. Última atualização: **2026-07-12** (fim da Etapa 6).

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
| 7 | Usuários | ⬜ **PRÓXIMA** |
| 8 | Clientes | ⬜ |
| 9 | Impressoras | ⬜ |
| 10 | Filamentos | ⬜ |
| 11 | Estoque | ⬜ |
| 12 | Pedidos | ⬜ |
| 13 | Orçamentos | ⬜ |
| 14 | Dashboard (gráficos + indicadores) | ⬜ |
| 15 | Financeiro | ⬜ |
| 16 | Relatórios (PDF) | ⬜ |
| 17 | Frontend React | ⬜ |
| 18 | Testes (JUnit, Mockito, integração) | ⬜ |
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
4. O serviço `frontend` do `docker-compose.yml` está **comentado** — descomentar na Etapa 17.
5. O git repo root é a **própria pasta do projeto** (`C:\repository\Print3d Manager ERP`), com remote `origin` → `github.com/JoaoAntonioOB/Print3dManagerERP`.
6. Segredo JWT tem **default de dev** no yml/compose (base64) — em produção deve vir do `.env` (`openssl rand -base64 48`).
7. Testes de contexto Spring reais (Testcontainers) ficam para a Etapa 18 — o teste atual é só de sanidade para o build passar sem banco.
8. Máquina: Windows 11, PowerShell 5.1. O terminal do usuário usa pt-BR.

---

## 8. Como retomar o desenvolvimento

1. Ler este arquivo e o `README.md`.
2. Confirmar o status da tabela da seção 5 com o usuário.
3. Implementar a próxima etapa pendente (**Etapa 7 — Usuários**: CRUD completo em `user/` — controller/service/dto/mapper —, primeiro uso de MapStruct e do padrão de paginação/filtros, `@PreAuthorize` por role (gestão restrita a ADMINISTRADOR), troca de senha, soft delete via `ativo`, e endpoint `/users/me` para o usuário logado).
4. Ao final de cada etapa: explicar decisões, validar build (`.\mvnw.cmd -B compile`) e **aguardar confirmação do usuário** antes da próxima etapa.
