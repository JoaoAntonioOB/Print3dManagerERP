/* eslint-disable react-refresh/only-export-components */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import toast from 'react-hot-toast';
import { api, EVENTO_SESSAO_EXPIRADA } from '../api/client';
import type { AuthResponse, UsuarioResumo } from '../api/types';
import { authStorage } from './auth-storage';

interface AuthContextValue {
  usuario: UsuarioResumo | null;
  autenticado: boolean;
  login: (email: string, senha: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioResumo | null>(() => authStorage.usuario());

  // Sessão derrubada pelo interceptor (refresh token vencido/reusado).
  useEffect(() => {
    const aoExpirar = () => {
      setUsuario(null);
      toast.error('Sua sessão expirou. Entre novamente.');
    };
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    return () => window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
  }, []);

  const login = useCallback(async (email: string, senha: string) => {
    const { data } = await api.post<AuthResponse>('/auth/login', { email, senha });
    authStorage.salvar(data);
    setUsuario(data.usuario);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = authStorage.refreshToken();
    try {
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch {
      // Sessão já inválida no servidor — só limpa localmente.
    } finally {
      authStorage.limpar();
      setUsuario(null);
    }
  }, []);

  const value = useMemo(
    () => ({ usuario, autenticado: usuario !== null, login, logout }),
    [usuario, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error('useAuth deve ser usado dentro de <AuthProvider>.');
  }
  return contexto;
}
