import { api } from './client';
import type { PageResponse } from './types';

export type PrintStatus = 'EM_ANDAMENTO' | 'CONCLUIDA' | 'FALHOU' | 'CANCELADA';

export const ROTULOS_STATUS_IMPRESSAO: Record<PrintStatus, string> = {
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
  FALHOU: 'Falhou',
  CANCELADA: 'Cancelada',
};

export const CORES_STATUS_IMPRESSAO: Record<
  PrintStatus,
  'info' | 'success' | 'error' | 'default'
> = {
  EM_ANDAMENTO: 'info',
  CONCLUIDA: 'success',
  FALHOU: 'error',
  CANCELADA: 'default',
};

export interface Impressao {
  id: number;
  impressoraId: number;
  impressoraNome: string;
  filamentoId: number | null;
  filamentoNome: string | null;
  itemPedidoId: number | null;
  nomePeca: string | null;
  pedidoNumero: string | null;
  usuarioId: number;
  usuarioNome: string;
  status: PrintStatus;
  iniciadoEm: string;
  finalizadoEm: string | null;
  tempoTotalMinutos: number | null;
  pesoUtilizadoG: number | null;
  consumoEnergiaKwh: number | null;
  custoTotal: number | null;
  motivoFalha: string | null;
  observacoes: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ImpressaoStartInput {
  impressoraId: number;
  filamentoId: number | null;
  itemPedidoId: number | null;
  observacoes: string | null;
}

/** Corpo de conclusão/cancelamento; falha exige também o motivo. */
export interface ImpressaoFinishInput {
  pesoUtilizadoG: number | null;
  consumoEnergiaKwh: number | null;
  observacoes: string | null;
}

export interface FiltrosImpressoes {
  impressoraId?: number | '';
  itemPedidoId?: number | '';
  status?: PrintStatus | '';
  page: number;
  size: number;
}

export const printsApi = {
  async listar(filtros: FiltrosImpressoes): Promise<PageResponse<Impressao>> {
    const { data } = await api.get<PageResponse<Impressao>>('/prints', {
      params: {
        impressoraId: filtros.impressoraId === '' ? undefined : filtros.impressoraId,
        itemPedidoId: filtros.itemPedidoId === '' ? undefined : filtros.itemPedidoId,
        status: filtros.status || undefined,
        page: filtros.page,
        size: filtros.size,
      },
    });
    return data;
  },

  async iniciar(impressao: ImpressaoStartInput): Promise<Impressao> {
    const { data } = await api.post<Impressao>('/prints', impressao);
    return data;
  },

  async concluir(id: number, corpo: ImpressaoFinishInput): Promise<Impressao> {
    const { data } = await api.patch<Impressao>(`/prints/${id}/concluir`, corpo);
    return data;
  },

  async falhar(
    id: number,
    corpo: ImpressaoFinishInput & { motivoFalha: string },
  ): Promise<Impressao> {
    const { data } = await api.patch<Impressao>(`/prints/${id}/falhar`, corpo);
    return data;
  },

  async cancelar(id: number): Promise<Impressao> {
    const { data } = await api.patch<Impressao>(`/prints/${id}/cancelar`);
    return data;
  },
};
