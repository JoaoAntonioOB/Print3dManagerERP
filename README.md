# Print3D Manager ERP

Sistema ERP completo para gerenciamento de empresas de impressão 3D — do orçamento à entrega do produto.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security, JWT (jjwt), Spring Data JPA, Hibernate, Flyway, MapStruct, Lombok, OpenAPI/Swagger |
| Banco de dados | PostgreSQL 16 |
| Frontend | React 19, TypeScript, Vite, MUI, TanStack Query, React Hook Form, Zod, Recharts |
| Infraestrutura | Docker, Docker Compose, NGINX |

## Estrutura do repositório

```
Print3d Manager ERP/
├── backend/                      # API REST (Spring Boot)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/print3dmanager/erp/
│       │   │   ├── Print3dManagerErpApplication.java
│       │   │   ├── config/       # Configurações (OpenAPI, CORS, Web, propriedades)
│       │   │   ├── security/     # Spring Security, JWT, filtros, refresh token
│       │   │   ├── common/       # Exceções globais, respostas padrão, paginação, base entity
│       │   │   ├── user/         # Módulo de usuários e papéis
│       │   │   ├── client/       # Módulo de clientes
│       │   │   ├── printer/      # Módulo de impressoras
│       │   │   ├── filament/     # Módulo de filamentos
│       │   │   ├── inventory/    # Módulo de estoque
│       │   │   ├── order/        # Módulo de pedidos, itens e histórico de impressões
│       │   │   ├── quote/        # Módulo de orçamentos (Strategy de precificação)
│       │   │   ├── financial/    # Módulo financeiro
│       │   │   ├── dashboard/    # Indicadores e agregações
│       │   │   ├── report/       # Relatórios em PDF
│       │   │   └── settings/     # Configurações da empresa e de precificação
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-prod.yml
│       │       └── db/migration/ # Migrações Flyway (V1__..., V2__...)
│       └── test/                 # Testes unitários e de integração (JUnit + Mockito + Testcontainers)
├── frontend/                     # SPA React (Etapa 17)
├── docker/                       # Dockerfiles, NGINX e compose auxiliares (Etapa 2)
├── .env.example                  # Modelo de variáveis de ambiente
└── docker-compose.yml            # Orquestração (Etapa 2)
```

Cada módulo de negócio segue internamente a mesma organização em camadas:

```
<módulo>/
├── controller/    # Endpoints REST (recebem/retornam apenas DTOs)
├── service/       # Regras de negócio, transações
├── repository/    # Spring Data JPA
├── model/         # Entidades JPA
├── dto/           # Java Records de entrada/saída
└── mapper/        # MapStruct (entidade <-> DTO)
```

## Decisões de arquitetura

1. **Monólito modular com package-by-feature** — cada domínio (pedidos, orçamentos, estoque…) é um pacote autocontido com suas próprias camadas. Para um ERP, isso mantém alta coesão, facilita a navegação e deixa o caminho aberto para extração futura de módulos, sem a complexidade prematura de microsserviços.
2. **DTOs como Java Records + MapStruct** — entidades JPA nunca saem da camada de serviço; records garantem imutabilidade e o MapStruct gera o mapeamento em tempo de compilação (sem reflection, com erro de build se um campo mudar).
3. **Flyway como fonte da verdade do schema** — `ddl-auto: validate` obriga o Hibernate a apenas validar; toda evolução de banco é versionada em `db/migration`.
4. **`open-in-view: false`** — evita lazy loading acidental na camada web, forçando consultas explícitas e previsíveis.
5. **Context path `/api`** — simplifica o roteamento no NGINX (`/api` → backend, `/` → frontend) e elimina ambiguidade de rotas.
6. **Configuração por variáveis de ambiente** — perfis `dev` (defaults locais) e `prod` (tudo via env, sem defaults para segredos), seguindo 12-factor.
7. **Timezone UTC no banco / JDBC** — datas são armazenadas em UTC e convertidas na apresentação, evitando bugs de horário de verão e migração de servidor.

## Como rodar (após a Etapa 2 — Docker)

```bash
cp .env.example .env   # ajuste os segredos
docker compose up -d --build
```

- Frontend: http://localhost
- API: http://localhost/api
- Swagger: http://localhost:8080/api/swagger-ui.html (ambiente de desenvolvimento)

### Desenvolvimento local sem Docker

```bash
# Backend (requer PostgreSQL local na porta 5432)
cd backend
mvn spring-boot:run

# Frontend (Etapa 17)
cd frontend
npm install
npm run dev
```

## Roadmap de implementação

1. ✅ Estrutura do projeto
2. ✅ Docker
3. ✅ Flyway
4. ✅ Banco de dados
5. ⬜ Spring Security
6. ⬜ JWT
7. ⬜ Usuários
8. ⬜ Clientes
9. ⬜ Impressoras
10. ⬜ Filamentos
11. ⬜ Estoque
12. ⬜ Pedidos
13. ⬜ Orçamentos
14. ⬜ Dashboard
15. ⬜ Financeiro
16. ⬜ Relatórios
17. ⬜ Frontend React
18. ⬜ Testes
19. ⬜ Melhorias finais
