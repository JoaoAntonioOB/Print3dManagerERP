import type { AuthResponse, UsuarioResumo } from '../api/types';

const CHAVE_ACCESS = 'print3d.accessToken';
const CHAVE_REFRESH = 'print3d.refreshToken';
const CHAVE_USUARIO = 'print3d.usuario';

/** Persistência da sessão em localStorage (multiabas e sobrevive a F5). */
export const authStorage = {
  salvar(auth: AuthResponse): void {
    localStorage.setItem(CHAVE_ACCESS, auth.accessToken);
    localStorage.setItem(CHAVE_REFRESH, auth.refreshToken);
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(auth.usuario));
  },

  limpar(): void {
    localStorage.removeItem(CHAVE_ACCESS);
    localStorage.removeItem(CHAVE_REFRESH);
    localStorage.removeItem(CHAVE_USUARIO);
  },

  accessToken(): string | null {
    return localStorage.getItem(CHAVE_ACCESS);
  },

  refreshToken(): string | null {
    return localStorage.getItem(CHAVE_REFRESH);
  },

  usuario(): UsuarioResumo | null {
    const bruto = localStorage.getItem(CHAVE_USUARIO);
    if (!bruto) {
      return null;
    }
    try {
      return JSON.parse(bruto) as UsuarioResumo;
    } catch {
      return null;
    }
  },
};
