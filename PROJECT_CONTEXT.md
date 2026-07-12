# PROJECT_CONTEXT.md — Print3D Manager ERP

> **Propósito deste arquivo:** contexto completo do projeto para retomada do desenvolvimento em novas sessões. Leia-o integralmente antes de escrever qualquer código. Última atualização: **2026-07-12** (fim da Etapa 2).

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
| 2 | Docker | ✅ Concluída (não testada — Docker não instalado na máquina) |
| 3 | Flyway | ⬜ **PRÓXIMA** |
| 4 | Banco de dados | ⬜ |
| 5 | Spring Security | ⬜ |
| 6 | JWT | ⬜ |
| 7 | Usuários | ⬜ |
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
│       ├── main/resources/db/migration/          # VAZIO — Flyway entra na Etapa 3
│       └── test/java/.../Print3dManagerErpApplicationTests.java  # teste de sanidade
└── frontend/
    ├── README.md              # placeholder — projeto Vite só na Etapa 17
    ├── Dockerfile             # Node 22 build → NGINX 1.27 (pronto, mas sem package.json ainda)
    ├── nginx.conf             # SPA + proxy /api + gzip + headers de segurança
    └── .dockerignore
```

**Build validado:** `mvnw compile` e `mvnw test` passam (Java 21 local).

### Configurações-chave já definidas (application.yml)
- `application.security.jwt.secret|access-token-expiration|refresh-token-expiration` (access 15 min, refresh 7 dias)
- `application.cors.allowed-origins` (dev: `http://localhost:5173`)
- `application.storage.upload-dir`
- Swagger UI: `/api/swagger-ui.html` · Actuator: `health,info,metrics`

---

## 7. Pendências e observações do ambiente

1. **Docker Desktop NÃO está instalado** na máquina do usuário. Instalar com `winget install Docker.DockerDesktop` antes da Etapa 4 (necessário para subir o Postgres e validar migrações). O compose nunca foi executado.
2. **`JAVA_HOME` não está configurado** no Windows. O JDK está em `C:\Program Files\Java\jdk-21.0.10` — foi setado manualmente na sessão para rodar o `mvnw`. Recomendar configurar como variável de sistema.
3. **Maven não está instalado** — usar sempre `.\mvnw.cmd` (Windows) dentro de `backend/`.
4. O serviço `frontend` do `docker-compose.yml` está **comentado** — descomentar na Etapa 17.
5. O **git repo root é `C:\repository`** (pai de vários projetos não relacionados do usuário). Cuidado com `git add .` fora da pasta do projeto.
6. Segredo JWT tem **default de dev** no yml/compose (base64) — em produção deve vir do `.env` (`openssl rand -base64 48`).
7. Testes de contexto Spring reais (Testcontainers) ficam para a Etapa 18 — o teste atual é só de sanidade para o build passar sem banco.
8. Máquina: Windows 11, PowerShell 5.1. O terminal do usuário usa pt-BR.

---

## 8. Como retomar o desenvolvimento

1. Ler este arquivo e o `README.md`.
2. Confirmar o status da tabela da seção 5 com o usuário.
3. Implementar a próxima etapa pendente (**Etapa 3 — Flyway**: migração `V1__` com schema base conforme entidades da seção 4).
4. Ao final de cada etapa: explicar decisões, validar build (`.\mvnw.cmd -B compile`) e **aguardar confirmação do usuário** antes da próxima etapa.
