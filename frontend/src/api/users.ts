import { api } from './client';
import type { PageResponse, Role } from './types';

export const ROTULOS_ROLE: Record<Role, string> = {
  ADMINISTRADOR: 'Administrador',
  OPERADOR: 'Operador',
  FINANCEIRO: 'Financeiro',
  VISUALIZADOR: 'Visualizador',
  CLIENTE: 'Cliente',
};

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  role: Role;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface UsuarioCreateInput {
  nome: string;
  email: string;
  senha: string;
  role: Role;
}

export interface UsuarioUpdateInput {
  nome: string;
  email: string;
  role: Role;
}

export interface FiltrosUsuarios {
  busca?: string;
  role?: Role | '';
  ativo?: boolean | '';
  page: number;
  size: number;
}

export const usersApi = {
  async listar(filtros: FiltrosUsuarios): Promise<PageResponse<Usuario>> {
    const { data } = await api.get<PageResponse<Usuario>>('/users', {
      params: {
        busca: filtros.busca || undefined,
        role: filtros.role || undefined,
        ativo: filtros.ativo === '' ? undefined : filtros.ativo,
        page: filtros.page,
        size: filtros.size,
        sort: 'nome,asc',
      },
    });
    return data;
  },

  async criar(usuario: UsuarioCreateInput): Promise<Usuario> {
    const { data } = await api.post<Usuario>('/users', usuario);
    return data;
  },

  async atualizar(id: number, usuario: UsuarioUpdateInput): Promise<Usuario> {
    const { data } = await api.put<Usuario>(`/users/${id}`, usuario);
    return data;
  },

  /** Soft delete: desativa o usuário e revoga as sessões dele. */
  async desativar(id: number): Promise<void> {
    await api.delete(`/users/${id}`);
  },

  async reativar(id: number): Promise<Usuario> {
    const { data } = await api.patch<Usuario>(`/users/${id}/ativar`);
    return data;
  },

  /** Troca a senha do próprio usuário; o backend revoga todas as sessões. */
  async trocarMinhaSenha(senhaAtual: string, novaSenha: string): Promise<void> {
    await api.patch('/users/me/senha', { senhaAtual, novaSenha });
  },
};
