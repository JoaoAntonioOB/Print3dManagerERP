import { api } from './client';
import type { PageResponse } from './types';

export type FilamentMaterial =
  | 'PLA'
  | 'ABS'
  | 'PETG'
  | 'TPU'
  | 'ASA'
  | 'NYLON'
  | 'RESINA'
  | 'OUTRO';

export type StockMovementType = 'ENTRADA' | 'SAIDA';

export interface Filamento {
  id: number;
  nome: string;
  marca: string | null;
  material: FilamentMaterial;
  cor: string | null;
  diametroMm: number;
  pesoBobinaG: number | null;
  custoPorKg: number | null;
  quantidadeEstoqueG: number;
  estoqueMinimoG: number;
  estoqueBaixo: boolean;
  temperaturaBico: number | null;
  temperaturaMesa: number | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface FilamentoInput {
  nome: string;
  marca: string | null;
  material: FilamentMaterial;
  cor: string | null;
  diametroMm: number | null;
  pesoBobinaG: number | null;
  custoPorKg: number | null;
  /** Só no cadastro — depois o estoque muda apenas por movimentação. */
  quantidadeEstoqueG?: number | null;
  estoqueMinimoG: number | null;
  temperaturaBico: number | null;
  temperaturaMesa: number | null;
}

export interface FiltrosFilamentos {
  busca?: string;
  material?: FilamentMaterial | '';
  ativo?: boolean | '';
  estoqueBaixo?: boolean;
  page: number;
  size: number;
}

export const filamentsApi = {
  async listar(filtros: FiltrosFilamentos): Promise<PageResponse<Filamento>> {
    const { data } = await api.get<PageResponse<Filamento>>('/filaments', {
      params: {
        busca: filtros.busca || undefined,
        material: filtros.material || undefined,
        ativo: filtros.ativo === '' ? undefined : filtros.ativo,
        estoqueBaixo: filtros.estoqueBaixo ? true : undefined,
        page: filtros.page,
        size: filtros.size,
        sort: 'nome,asc',
      },
    });
    return data;
  },

  async criar(filamento: FilamentoInput): Promise<Filamento> {
    const { data } = await api.post<Filamento>('/filaments', filamento);
    return data;
  },

  async atualizar(id: number, filamento: FilamentoInput): Promise<Filamento> {
    const { quantidadeEstoqueG: _ignorado, ...corpo } = filamento;
    const { data } = await api.put<Filamento>(`/filaments/${id}`, corpo);
    return data;
  },

  async movimentarEstoque(
    id: number,
    tipo: StockMovementType,
    quantidadeG: number,
  ): Promise<Filamento> {
    const { data } = await api.patch<Filamento>(`/filaments/${id}/estoque`, {
      tipo,
      quantidadeG,
    });
    return data;
  },

  async desativar(id: number): Promise<void> {
    await api.delete(`/filaments/${id}`);
  },

  async reativar(id: number): Promise<Filamento> {
    const { data } = await api.patch<Filamento>(`/filaments/${id}/ativar`);
    return data;
  },
};
