import { dispararDownloadBlob, nomeDoContentDisposition } from '../lib/download';
import { api } from './client';
import type { PageResponse } from './types';

export type OrderStatus =
  | 'PENDENTE'
  | 'EM_PRODUCAO'
  | 'CONCLUIDO'
  | 'ENTREGUE'
  | 'CANCELADO';

export const ROTULOS_STATUS_PEDIDO: Record<OrderStatus, string> = {
  PENDENTE: 'Pendente',
  EM_PRODUCAO: 'Em produção',
  CONCLUIDO: 'Concluído',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
};

export const CORES_STATUS_PEDIDO: Record<
  OrderStatus,
  'default' | 'info' | 'success' | 'secondary' | 'error'
> = {
  PENDENTE: 'default',
  EM_PRODUCAO: 'info',
  CONCLUIDO: 'secondary',
  ENTREGUE: 'success',
  CANCELADO: 'error',
};

/** Transições permitidas pela máquina de estados do backend. */
export const TRANSICOES_PEDIDO: Record<OrderStatus, OrderStatus[]> = {
  PENDENTE: ['EM_PRODUCAO', 'CANCELADO'],
  EM_PRODUCAO: ['CONCLUIDO', 'CANCELADO'],
  CONCLUIDO: ['ENTREGUE'],
  ENTREGUE: [],
  CANCELADO: [],
};

export interface PedidoResumo {
  id: number;
  numero: string;
  clienteId: number;
  clienteNome: string;
  status: OrderStatus;
  dataEntregaPrevista: string | null;
  dataEntregaRealizada: string | null;
  valorTotal: number;
  desconto: number;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ItemPedido {
  id: number;
  filamentoId: number | null;
  filamentoNome: string | null;
  nomePeca: string;
  descricao: string | null;
  quantidade: number;
  pesoEstimadoG: number | null;
  tempoImpressaoMinutos: number | null;
  precoUnitario: number;
  subtotal: number;
  /** Caminho no armazenamento (modelos/<uuid>_<nome>); omitido quando não há arquivo. */
  arquivoModelo?: string | null;
}

/** Nome amigável do arquivo de modelo: remove o diretório e o prefixo UUID. */
export function nomeDoArquivoModelo(caminho: string): string {
  const nome = caminho.slice(caminho.lastIndexOf('/') + 1);
  const separador = nome.indexOf('_');
  return separador < 0 ? nome : nome.slice(separador + 1);
}

export interface Pedido extends Omit<PedidoResumo, never> {
  usuarioId: number;
  usuarioNome: string;
  observacoes: string | null;
  itens: ItemPedido[];
}

export interface ItemPedidoInput {
  id?: number | null;
  filamentoId: number | null;
  nomePeca: string;
  descricao: string | null;
  quantidade: number | null;
  pesoEstimadoG: number | null;
  tempoImpressaoMinutos: number | null;
  precoUnitario: number;
}

export interface PedidoInput {
  clienteId: number;
  dataEntregaPrevista: string | null;
  desconto: number | null;
  observacoes: string | null;
  itens: ItemPedidoInput[];
}

export interface FiltrosPedidos {
  busca?: string;
  status?: OrderStatus | '';
  page: number;
  size: number;
}

export interface TransacaoFinanceira {
  id: number;
  tipo: 'RECEITA' | 'DESPESA';
  categoria: string;
  descricao: string;
  valor: number;
  status: string;
}

export const ordersApi = {
  async listar(filtros: FiltrosPedidos): Promise<PageResponse<PedidoResumo>> {
    const { data } = await api.get<PageResponse<PedidoResumo>>('/orders', {
      params: {
        busca: filtros.busca || undefined,
        status: filtros.status || undefined,
        page: filtros.page,
        size: filtros.size,
      },
    });
    return data;
  },

  async buscarPorId(id: number): Promise<Pedido> {
    const { data } = await api.get<Pedido>(`/orders/${id}`);
    return data;
  },

  async criar(pedido: PedidoInput): Promise<Pedido> {
    const { data } = await api.post<Pedido>('/orders', pedido);
    return data;
  },

  async atualizar(id: number, pedido: PedidoInput): Promise<Pedido> {
    const { data } = await api.put<Pedido>(`/orders/${id}`, pedido);
    return data;
  },

  async alterarStatus(id: number, status: OrderStatus): Promise<Pedido> {
    const { data } = await api.patch<Pedido>(`/orders/${id}/status`, { status });
    return data;
  },

  async excluir(id: number): Promise<void> {
    await api.delete(`/orders/${id}`);
  },

  async faturar(id: number): Promise<TransacaoFinanceira> {
    const { data } = await api.post<TransacaoFinanceira>(`/orders/${id}/faturar`);
    return data;
  },

  /** Anexa (ou substitui) o modelo 3D do item — só pedidos PENDENTES. */
  async anexarArquivoItem(
    pedidoId: number,
    itemId: number,
    arquivo: File,
  ): Promise<ItemPedido> {
    const form = new FormData();
    form.append('arquivo', arquivo);
    const { data } = await api.post<ItemPedido>(
      `/orders/${pedidoId}/itens/${itemId}/arquivo`,
      form,
    );
    return data;
  },

  /** Baixa o modelo 3D do item com o nome vindo do Content-Disposition. */
  async baixarArquivoItem(pedidoId: number, itemId: number): Promise<void> {
    const { data, headers } = await api.get<Blob>(
      `/orders/${pedidoId}/itens/${itemId}/arquivo`,
      { responseType: 'blob' },
    );
    dispararDownloadBlob(
      data,
      nomeDoContentDisposition(
        headers['content-disposition'] as string | undefined,
        'modelo.stl',
      ),
    );
  },

  /** Remove o modelo 3D do item — só pedidos PENDENTES. */
  async removerArquivoItem(pedidoId: number, itemId: number): Promise<void> {
    await api.delete(`/orders/${pedidoId}/itens/${itemId}/arquivo`);
  },
};
