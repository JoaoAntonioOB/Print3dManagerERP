## Alteração — Correção MÉDIA #12 (InventoryPage) + MÉDIA #18 (CVE react-router-dom)

**Arquivos alterados/criados:**
- `frontend/src/pages/inventory/InventoryPage.tsx` (linha 228): `item.custoUnitario !== null` → `item.custoUnitario != null`.
- `frontend/package.json`: `react-router-dom` `^7.18.1` → `^7.18.2`.
- `frontend/package-lock.json`: lockfile atualizado (`react-router` e `react-router-dom` para `7.18.2`).

**Padrão seguido de:** o próprio arquivo (`item.localizacao ?? '—'`, logo abaixo na mesma tabela) e outros usos idênticos já corretos no projeto, ex. `frontend/src/pages/filaments/FilamentsPage.tsx:254` que já usava a forma `!== null` — este é o único outro ponto do código com o mesmo padrão de formatação de moeda opcional; verifiquei e ele usa `!== null`, não `!= null`, ou seja **não é o padrão-alvo, mas potencialmente sofre da mesma classe de bug** (ver "Achados" abaixo). Busquei todos os usos de `!== null` no projeto (`Grep` em `src/`) e confirmei que os demais (ex. `usuario !== null`, `*ParaDesativar !== null`, `orcamentoId !== null`) comparam contra estado local do React inicializado explicitamente com `null` (não campo vindo de resposta de API serializada pelo Jackson), portanto não sofrem do gotcha `undefined`/Jackson e não precisam de alteração.

**Build/lint:** passou.
- `npm run build` (`tsc -b && vite build`): sucesso, sem erros de tipo. Warning pré-existente e não relacionado sobre chunk > 500kB (já existia antes da mudança).
- `npm run lint` (`oxlint`): sem erros.

**npm audit:**
- Antes: `GHSA-qwww-vcr4-c8h2` (high, CWE-352 CSRF bypass em RSC Mode) em `react-router`/`react-router-dom`, range vulnerável `7.12.0 - 7.18.1`. Total reportado: 4 vulnerabilidades (1 moderate, 3 high).
- Depois de `npm install` com `react-router-dom@^7.18.2`: `GHSA-qwww-vcr4-c8h2` **não aparece mais** no relatório. Restam 2 vulnerabilidades transitivas não relacionadas a esta tarefa e sem `isDirect: true` no `react-router`: `nanoid` (high, `GHSA-2v37-7h3g-55p8`, `<3.3.18`) e `postcss` (moderate, `GHSA-fxqj-rqcc-2cmp`, `<=8.5.22`) — ambas vindas de devDependencies/toolchain (não de `react-router-dom`), fora do escopo pedido nesta tarefa.

**Como validar no browser:**
1. `docker compose up -d` na raiz do repo (ou `npm run dev` em `frontend/` com backend acessível via proxy) e abrir `http://localhost` (ou `http://localhost:5173` em dev).
2. Login com `admin@print3d.com` / `admin123`.
3. Ir em **Estoque** (`/estoque`) e conferir a coluna "Custo unitário": itens com custo definido mostram `R$ x,xx`; itens sem custo (campo ausente/`undefined` na resposta) mostram `—` em vez de "R$ NaN".
4. Navegar entre pelo menos 4 páginas (`/clientes`, `/orcamentos`, `/pedidos`, `/estoque`, dashboard `/`) e fazer logout, confirmando que o roteamento client-side (react-router-dom 7.18.2) continua funcionando sem erros no console.

**Validação executada nesta sessão (browser real, Edge via playwright-core, headless):**
- Rebuild da imagem Docker do frontend (`docker compose build frontend && docker compose up -d frontend`) para testar o bundle já com as mudanças, servido por `http://localhost` (nginx, ambiente equivalente à produção).
- Script `drive-router-smoke.mjs` (scratchpad de sessão) executou: abrir `/login` → login com `admin@print3d.com`/`admin123` → redirecionamento para `/` (dashboard) → navegação client-side para `/clientes`, `/orcamentos`, `/pedidos`, `/estoque` (cada uma renderizando conteúdo específico da página, não apenas o shell) → voltar para `/` via clique em link do menu (não reload de página) → logout via menu do usuário → redirecionamento para `/login`. **Resultado: 8/8 passos OK.**
- Verificação específica da tela de Estoque: `body.innerText` não contém `"NaN"`; item existente com custo definido mostra `R$ 2,35` corretamente.
- Durante a validação, o container `print3d-backend` entrou em crash-loop transitório (falha de autenticação Postgres seguida de recuperação) — **pré-existente e não relacionado a esta mudança** (frontend não toca em credenciais de banco); após o backend estabilizar (`docker ps` reportando `healthy`), o smoke test rodou limpo. Sinalizado como achado informativo abaixo para o time de `devops`/`backend` investigar a causa do crash-loop.

**Achados (severidade):**
- BAIXO — `frontend/src/pages/filaments/FilamentsPage.tsx:254` usa `filamento.custoPorKg !== null ? moeda.format(...) : '—'`, o mesmo padrão originalmente reportado como bug no `InventoryPage.tsx`. Não foi alterado nesta tarefa por estar fora do escopo do achado MÉDIO #12 (que especificava apenas `InventoryPage.tsx`), mas é o mesmo gotcha Jackson (`undefined` vs `null`) e deveria receber a mesma correção (`!== null` → `!= null`) em uma tarefa futura.
- INFORMATIVO — `npm audit` ainda reporta `nanoid` (high) e `postcss` (moderate) como vulnerabilidades transitivas, não relacionadas a `react-router-dom` nem ao escopo desta tarefa. `npm audit fix` resolveria automaticamente se aprovado (não executado aqui para não alterar dependências fora do pedido).
- INFORMATIVO — Durante a validação, o container `print3d-backend` (ambiente de desenvolvimento local, não relacionado a esta mudança) apresentou crash-loop transitório por falha de autenticação com o Postgres, revertendo sozinho após alguns ciclos. Não investigado a fundo por estar fora do escopo deste agente (backend/infra).

**Encaminhamentos:**
- `frontend` (tarefa futura, não crítica): aplicar a mesma correção `!== null` → `!= null` em `frontend/src/pages/filaments/FilamentsPage.tsx:254` (mesmo gotcha Jackson).
- `devops`/`backend`: investigar o crash-loop intermitente observado no container `print3d-backend` local (falha de autenticação Postgres em alguns ciclos de start, seguida de recuperação) — pode ser sintoma de uma condição de corrida entre o backend e o healthcheck do Postgres, ou de credenciais desincronizadas no ambiente local.
- Opcional/baixa prioridade: `npm audit fix` para `nanoid`/`postcss` (transitivas), se o time quiser zerar o `npm audit` por completo — não solicitado nesta tarefa.
