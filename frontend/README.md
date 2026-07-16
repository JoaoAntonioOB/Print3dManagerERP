# Print3D Manager ERP — Frontend

SPA React do Print3D Manager ERP (Etapa 17 — em construção).

## Stack

- React 19 + TypeScript + Vite
- React Router · Axios · TanStack Query
- React Hook Form + Zod
- Material UI (MUI) · Recharts · React Hot Toast

## Como rodar em desenvolvimento

Pré-requisito: backend no ar em `http://localhost:8080` (via `docker compose up -d`
na raiz do projeto, ou `mvnw spring-boot:run` com perfil `dev` em `backend/`).

```bash
npm install
npm run dev     # http://localhost:5173
```

O Vite proxya `/api` para o backend (removendo o header `Origin` — em produção o
NGINX faz esse papel), então a aplicação nunca precisa saber o host da API.

Login inicial: `admin@print3d.com` / `admin123` (migração V11 — trocar em produção).

## Scripts

| Script | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com HMR |
| `npm run build` | Typecheck (`tsc -b`) + build de produção em `dist/` |
| `npm run lint` | oxlint |
| `npm run preview` | Serve o build de produção localmente |

## Estrutura

```
src/
├── api/        # Cliente Axios (interceptors de token/refresh) e tipos da API
├── auth/       # AuthProvider, guarda de rotas e persistência da sessão
├── layout/     # AppLayout: AppBar + menu lateral por perfil
├── pages/      # Telas (Login, Dashboard, placeholders dos módulos)
├── theme.ts    # Tema MUI (pt-BR, cores do sistema)
├── App.tsx     # Rotas
└── main.tsx    # Providers (tema, React Query, auth, router, toaster)
```

## Autenticação

- Tokens em `localStorage`; o interceptor Axios anexa o `Bearer` e, num 401,
  renova via `POST /auth/refresh` (uma renovação por vez — requisições
  concorrentes aguardam a mesma promise) e repete a chamada original.
- Refresh inválido/expirado limpa a sessão e volta ao login com aviso.
- Menu lateral filtrado pelo perfil (`role`) do usuário logado — ex.:
  **Usuários** só aparece para ADMINISTRADOR.
