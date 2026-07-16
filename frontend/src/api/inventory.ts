import { api } from './client';
import type { StockMovementType } from './filaments';
import type { PageResponse } from './types';

export interface ItemEstoque {
  id: number;
  nome: string;
  descricao: string | null;
  categoria: string | null;
  quantidade: number;
  unidadeMedida: string;
  quantidadeMinima: number;
  estoqueBaixo: boolean;
  custoUnitario: number | null;
  localizacao: string | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ItemEstoqueInput {
  nome: string;
  descricao: string | null;
  categoria: string | null;
  /** Só no cadastro — depois a quantidade muda apenas por movimentação. */
  quantidade?: number | null;
  unidadeMedida: string | null;
  quantidadeMinima: number | null;
  custoUnitario: number | null;
  localizacao: string | null;
}

export interface FiltrosEstoque {
  busca?: string;
  categoria?: string;
  ativo?: boolean | '';
  estoqueBaixo?: boolean;
  page: number;
  size: number;
}

export const inventoryApi = {
  async listar(filtros: FiltrosEstoque): Promise<PageResponse<ItemEstoque>> {
    const { data } = await api.get<PageResponse<ItemEstoque>>('/inventory', {
      params: {
        busca: filtros.busca || undefined,
        categoria: filtros.categoria || undefined,
        ativo: filtros.ativo === '' ? undefined : filtros.ativo,
        estoqueBaixo: filtros.estoqueBaixo ? true : undefined,
        page: filtros.page,
        size: filtros.size,
        sort: 'nome,asc',
      },
    });
    return data;
  },

  async criar(item: ItemEstoqueInput): Promise<ItemEstoque> {
    const { data } = await api.post<ItemEstoque>('/inventory', item);
    return data;
  },

  async atualizar(id: number, item: ItemEstoqueInput): Promise<ItemEstoque> {
    const { quantidade: _ignorada, ...corpo } = item;
    const { data } = await api.put<ItemEstoque>(`/inventory/${id}`, corpo);
    return data;
  },

  async movimentarEstoque(
    id: number,
    tipo: StockMovementType,
    quantidade: number,
  ): Promise<ItemEstoque> {
    const { data } = await api.patch<ItemEstoque>(`/inventory/${id}/estoque`, {
      tipo,
      quantidade,
    });
    return data;
  },

  async desativar(id: number): Promise<void> {
    await api.delete(`/inventory/${id}`);
  },

  async reativar(id: number): Promise<ItemEstoque> {
    const { data } = await api.patch<ItemEstoque>(`/inventory/${id}/ativar`);
    return data;
  },
};
