import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { authStorage } from '../auth/auth-storage';
import type { ApiErrorResponse, AuthResponse } from './types';

/** Disparado quando a sessão morre de vez (refresh falhou) — o AuthProvider escuta. */
export const EVENTO_SESSAO_EXPIRADA = 'print3d:sessao-expirada';

/**
 * Cliente HTTP da API. Em dev o Vite proxya /api para o backend; em produção
 * o NGINX faz o mesmo — a aplicação nunca conhece o host do backend.
 */
export const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
  const token = authStorage.accessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Compartilhado entre requisições concorrentes: um único refresh por vez. */
let refreshEmAndamento: Promise<string> | null = null;

async function renovarAccessToken(): Promise<string> {
  const refreshToken = authStorage.refreshToken();
  if (!refreshToken) {
    throw new Error('Sessão expirada.');
  }
  // axios "cru" de propósito: o interceptor da api não deve reagir a este 401.
  const { data } = await axios.post<AuthResponse>('/api/auth/refresh', { refreshToken });
  authStorage.salvar(data);
  return data.accessToken;
}

api.interceptors.response.use(undefined, async (erro: AxiosError<ApiErrorResponse>) => {
  const original = erro.config as (InternalAxiosRequestConfig & { _retentada?: boolean }) | undefined;
  const url = original?.url ?? '';
  const ehRotaDeAuth = url.startsWith('/auth');
  if (erro.response?.status !== 401 || !original || original._retentada || ehRotaDeAuth) {
    throw erro;
  }

  original._retentada = true;
  try {
    refreshEmAndamento = refreshEmAndamento ?? renovarAccessToken();
    const token = await refreshEmAndamento;
    original.headers.Authorization = `Bearer ${token}`;
    return await api(original);
  } catch (erroRefresh) {
    authStorage.limpar();
    window.dispatchEvent(new Event(EVENTO_SESSAO_EXPIRADA));
    throw erroRefresh;
  } finally {
    refreshEmAndamento = null;
  }
});

/** Extrai a mensagem da resposta de erro padrão da API para exibir em toast. */
export function mensagemDeErro(erro: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(erro)) {
    const corpo = erro.response?.data;
    if (corpo?.errors?.length) {
      return corpo.errors.map((e) => e.mensagem).join(' ');
    }
    if (corpo?.message) {
      return corpo.message;
    }
    if (!erro.response) {
      return 'Não foi possível conectar ao servidor.';
    }
  }
  return 'Erro inesperado. Tente novamente.';
}
