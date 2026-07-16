import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Em dev o Vite faz o papel do NGINX: repassa /api para o backend local.
    // O header Origin é removido para a chamada chegar como same-origin —
    // sem isso o backend do Docker (CORS restrito a http://localhost)
    // rejeita o Origin http://localhost:5173 com 403.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
    },
  },
});
