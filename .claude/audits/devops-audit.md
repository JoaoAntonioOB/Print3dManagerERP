## Infraestrutura — Postgres exposto no host + senha default fraca (achado ALTO #9) corrigidos

**Contexto:** correção do achado ALTO #9 da auditoria de 2026-08-16
(`FINAL-PLAN.md`), irmão do achado CRÍTICO da porta 8080 do backend
(já corrigido em rodada anterior desta mesma sessão).

### O que foi alterado

**1. `docker-compose.yml`**
- `postgres.ports`: `"5432:5432"` (publicado em `0.0.0.0`, alcançável de
  fora da máquina) → **`"127.0.0.1:5432:5432"`** (só loopback). Confirmado
  com `docker port print3d-postgres` → `5432/tcp -> 127.0.0.1:5432` e com
  `Test-NetConnection` (ver evidências abaixo).
  - Decisão: **restringi a loopback em vez de remover a publicação por
    completo** (diferente do que fiz com a porta 8080 do backend). Motivo:
    ao contrário do backend, existe um fluxo de dev **legítimo e já
    documentado** no `README.md` ("Desenvolvimento local sem Docker") em
    que o backend roda via `mvnw`/`mvn spring-boot:run` **diretamente no
    host** (fora de container) e precisa alcançar o Postgres em
    `localhost:5432` — `application-dev.yml` tem `DATABASE_URL` default
    `jdbc:postgresql://localhost:5432/...`. Remover a publicação por
    completo quebraria esse fluxo; restringir a `127.0.0.1` preserva o
    fluxo de dev e ainda assim fecha o acesso externo, que era o risco real
    do achado.
- `POSTGRES_PASSWORD` e `DATABASE_PASSWORD` (backend): removido o default
  fraco `:-print3d` → trocado por `${POSTGRES_PASSWORD:?mensagem}`
  (sintaxe de variável obrigatória do Compose Spec). Sem essa variável no
  `.env`, `docker compose config`/`up` **falha explicitamente** com
  mensagem clara, em vez de subir o banco com senha previsível.
  - Decisão: **fail-fast, sem manter default documentado.** O README já
    instrui `cp .env.example .env` como primeiro passo antes de
    `docker compose up`, então isso não quebra o fluxo "clonar e rodar"
    documentado — só passa a falhar (de forma clara) para quem pula esse
    passo, que é exatamente o comportamento desejado pela auditoria.
    Ajustei a seção "Desenvolvimento local sem Docker" do `README.md` para
    deixar explícito que esse fluxo também depende de `.env` (ver abaixo).
- **Achado adicional encontrado (não solicitado, mas dentro do escopo
  autorizado):** `JWT_SECRET` no `docker-compose.yml` também tinha um
  default hardcoded (`cHJpbnQzZC1tYW5hZ2VyLWVycC1kZXYtc2VjcmV0LWtleS0yNTYt...`)
  que **anularia silenciosamente** qualquer validação fail-fast que o
  agente `backend` esteja implementando em paralelo em
  `application-prod.yml`: mesmo que o Spring exija `JWT_SECRET` sem
  default, a variável de ambiente chegaria sempre preenchida pelo próprio
  Compose (com ou sem `.env`), porque a interpolação `${JWT_SECRET:-...}`
  resolve *antes* do Spring decidir se o valor está ausente. Troquei para
  `${JWT_SECRET:?mensagem}` (mesma obrigatoriedade fail-fast). **Não mexi
  em nenhum arquivo Java nem em `application*.yml`** — só removi o
  fallback correspondente que estava no compose.

**2. `.env.example`**
- Comentários adicionados deixando explícito que `POSTGRES_PASSWORD`,
  `DATABASE_PASSWORD` e `JWT_SECRET` agora são **obrigatórios** (o compose
  falha sem eles). Nenhum valor de segredo real foi commitado — mantidos
  os placeholders `troque-esta-senha` / `troque-por-um-segredo-base64-de-48-bytes`
  já existentes.

**3. `README.md`**
- Seção "Desenvolvimento local sem Docker" expandida: agora explica que o
  profile `dev` do backend assume Postgres em `localhost:5432` com
  `print3d`/`print3d` (só para essa conveniência local), como subir só o
  serviço `postgres` via `docker compose up -d postgres` com `.env`
  configurado com essas credenciais, e que a porta 5432 agora só é
  publicada em `127.0.0.1`.

### Validado com
- `docker compose config` **sem** `.env` → erro fail-fast confirmado:
  `error while interpolating services.postgres.environment.POSTGRES_PASSWORD:
  required variable POSTGRES_PASSWORD is missing a value: defina
  POSTGRES_PASSWORD no .env (...) - sem default por seguranca`.
- `docker compose config` **com** `.env` de teste → interpolação correta,
  `ports` do serviço `postgres` resolvido como
  `host_ip: 127.0.0.1` (sem `0.0.0.0`).
- `docker compose up -d` com imagens já buildadas (rebuild completo do
  backend está bloqueado no momento por um erro de compilação de teste
  pré-existente e não relacionado — `PrinterServiceTest.java` incompatível
  com a assinatura atual do record `PrinterResponse`, introduzido por
  trabalho paralelo de outro agente no domínio `backend`; **fora do meu
  escopo, encaminhado abaixo**) → `docker compose ps` mostra os 3 serviços
  `healthy`:
  ```
  print3d-backend    Up (healthy)   8080/tcp
  print3d-frontend   Up (healthy)   0.0.0.0:80->80/tcp
  print3d-postgres   Up (healthy)   127.0.0.1:5432->5432/tcp
  ```
- `curl http://localhost/` → 200; `curl http://localhost/api/actuator/health`
  → `{"status":"UP"}` (fluxo completo via NGINX → backend → Postgres
  funcionando com senha vinda só do `.env`, sem default no compose).
- `docker port print3d-postgres` → `5432/tcp -> 127.0.0.1:5432` (confirma
  não há bind em `0.0.0.0`).
- `Test-NetConnection -ComputerName 192.168.1.67 -Port 5432` (IP da LAN do
  host) → `TcpTestSucceeded: False`. `Test-NetConnection -ComputerName
  127.0.0.1 -Port 5432` → `TcpTestSucceeded: True`. Confirma: acessível só
  do próprio host, fechado para a rede.
- `Test-NetConnection` na porta 8080 (LAN e loopback) → `False` em ambos,
  confirmando que a correção anterior (porta 8080) segue intacta.
- `.env` de teste usado só localmente durante a validação, **removido ao
  final** (`rm .env`) — nunca commitado; confirmado via `git check-ignore
  -v .env` que o `.gitignore` já cobre `.env`.

### Impacto em produção
- Em produção real, `docker-compose.yml` deixa de subir com senha
  previsível por acidente: sem `POSTGRES_PASSWORD`/`JWT_SECRET` definidos
  via segredo real (ex.: `openssl rand -base64 48`, gerenciador de
  segredos do orquestrador), a stack simplesmente não sobe — comportamento
  fail-fast alinhado ao restante da auditoria (12-factor, sem default de
  segredo em prod).
- Postgres deixa de estar alcançável pela rede/host externo mesmo em
  ambientes onde o firewall da máquina não bloqueia a porta — reduz
  superfície de ataque sem quebrar nenhum fluxo de dev documentado.

### Achados (severidade)
- **[RESOLVIDO] ALTO** — Postgres publicado em `0.0.0.0:5432` com senha
  default fraca (`print3d`). Corrigido: bind só em `127.0.0.1` + senha
  obrigatória sem default.
- **[RESOLVIDO] MÉDIO (adicional, encontrado durante a correção)** —
  `JWT_SECRET` tinha default hardcoded no `docker-compose.yml` que
  mascarava qualquer validação fail-fast feita no lado Spring
  (`application-prod.yml`), pois o Compose sempre entregava um valor
  não-nulo para a variável de ambiente independentemente do `.env`.
  Corrigido com `${JWT_SECRET:?mensagem}`.
- **INFORMATIVO** — build completo do backend (`docker compose up -d
  --build`) está quebrado agora por um erro de compilação de teste
  (`PrinterServiceTest.java` vs. record `PrinterResponse`) introduzido por
  trabalho paralelo de outro agente, não relacionado a infraestrutura.
  Validei a stack usando as imagens já existentes (buildadas antes dessa
  quebra) — suficiente para confirmar rede/portas/env, mas a stack não
  pode ser rebuildada do zero até esse erro ser corrigido no domínio
  `backend`.

### Encaminhamentos
- `backend`: `backend/src/test/java/com/print3dmanager/erp/printer/service/PrinterServiceTest.java`
  não compila contra a assinatura atual do record `PrinterResponse`
  (`constructor PrinterResponse ... cannot be applied to given types`) —
  bloqueia `docker compose up -d --build` no estado atual do working tree.
  Não é um problema de infraestrutura; encaminho para o agente responsável
  pelo domínio `backend`/`printer` corrigir o teste ou o record antes do
  próximo build completo.
- Nenhum outro encaminhamento relacionado a este achado.
