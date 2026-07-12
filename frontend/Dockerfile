# ==========================================================
# Estágio 1 — Build (Node 22 + Vite)
# ==========================================================
FROM node:22-alpine AS build
WORKDIR /app

# Cache de dependências: package*.json muda menos que o código-fonte
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ==========================================================
# Estágio 2 — Runtime (NGINX)
# ==========================================================
FROM nginx:1.27-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
