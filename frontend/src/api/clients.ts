import { api } from './client';
import type { PageResponse } from './types';

export type PersonType = 'FISICA' | 'JURIDICA';

export interface Endereco {
  logradouro?: string | null;
  numero?: string | null;
  complemento?: string | null;
  bairro?: string | null;
  cidade?: string | null;
  estado?: string | null;
  cep?: string | null;
}

export interface Cliente {
  id: number;
  nome: string;
  email: string | null;
  telefone: string | null;
  tipoPessoa: PersonType;
  cpfCnpj: string | null;
  endereco: Endereco | null;
  observacoes: string | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ClienteInput {
  nome: string;
  email: string | null;
  telefone: string | null;
  tipoPessoa: PersonType;
  cpfCnpj: string | null;
  endereco: Endereco | null;
  observacoes: string | null;
}

export interface FiltrosClientes {
  busca?: string;
  tipoPessoa?: PersonType | '';
  ativo?: boolean | '';
  page: number;
  size: number;
}

export const clientsApi = {
  async listar(filtros: FiltrosClientes): Promise<PageResponse<Cliente>> {
    const { data } = await api.get<PageResponse<Cliente>>('/clients', {
      params: {
        busca: filtros.busca || undefined,
        tipoPessoa: filtros.tipoPessoa || undefined,
        ativo: filtros.ativo === '' ? undefined : filtros.ativo,
        page: filtros.page,
        size: filtros.size,
        sort: 'nome,asc',
      },
    });
    return data;
  },

  async criar(cliente: ClienteInput): Promise<Cliente> {
    const { data } = await api.post<Cliente>('/clients', cliente);
    return data;
  },

  async atualizar(id: number, cliente: ClienteInput): Promise<Cliente> {
    const { data } = await api.put<Cliente>(`/clients/${id}`, cliente);
    return data;
  },

  async desativar(id: number): Promise<void> {
    await api.delete(`/clients/${id}`);
  },

  async reativar(id: number): Promise<Cliente> {
    const { data } = await api.patch<Cliente>(`/clients/${id}/ativar`);
    return data;
  },
};
