import { api } from './client';
import type { PageResponse } from './types';

export type TransactionType = 'RECEITA' | 'DESPESA';
export type TransactionStatus = 'PENDENTE' | 'PAGA' | 'CANCELADA';

export const ROTULOS_TIPO_TRANSACAO: Record<TransactionType, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
};

export const ROTULOS_STATUS_TRANSACAO: Record<TransactionStatus, string> = {
  PENDENTE: 'Pendente',
  PAGA: 'Paga',
  CANCELADA: 'Cancelada',
};

export const CORES_STATUS_TRANSACAO: Record<
  TransactionStatus,
  'warning' | 'success' | 'default'
> = {
  PENDENTE: 'warning',
  PAGA: 'success',
  CANCELADA: 'default',
};

/** PENDENTE→PAGA|CANCELADA; PAGA→PENDENTE (estorno); CANCELADA é terminal. */
export const TRANSICOES_TRANSACAO: Record<TransactionStatus, TransactionStatus[]> = {
  PENDENTE: ['PAGA', 'CANCELADA'],
  PAGA: ['PENDENTE'],
  CANCELADA: [],
};

export interface Transacao {
  id: number;
  tipo: TransactionType;
  categoria: string;
  descricao: string;
  /** Sempre positivo — o sinal é dado pelo tipo. */
  valor: number;
  dataTransacao: string;
  status: TransactionStatus;
  formaPagamento: string | null;
  pedidoId: number | null;
  pedidoNumero: string | null;
  clienteId: number | null;
  clienteNome: string | null;
  observacoes: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface TransacaoInput {
  tipo: TransactionType;
  categoria: string;
  descricao: string;
  valor: number;
  dataTransacao: string;
  /** Só no cadastro (omitido = PENDENTE); depois muda apenas pelo endpoint de status. */
  status?: TransactionStatus | null;
  formaPagamento: string | null;
  pedidoId: number | null;
  clienteId: number | null;
  observacoes: string | null;
}

export interface FiltrosTransacoes {
  busca?: string;
  tipo?: TransactionType | '';
  status?: TransactionStatus | '';
  page: number;
  size: number;
}

export interface ResumoFinanceiro {
  de: string;
  ate: string;
  receitasPagas: number;
  receitasPendentes: number;
  despesasPagas: number;
  despesasPendentes: number;
  saldoRealizado: number;
  saldoPrevisto: number;
}

export const financialApi = {
  async listar(filtros: FiltrosTransacoes): Promise<PageResponse<Transacao>> {
    const { data } = await api.get<PageResponse<Transacao>>('/financial/transactions', {
      params: {
        busca: filtros.busca || undefined,
        tipo: filtros.tipo || undefined,
        status: filtros.status || undefined,
        page: filtros.page,
        size: filtros.size,
      },
    });
    return data;
  },

  async criar(transacao: TransacaoInput): Promise<Transacao> {
    const { data } = await api.post<Transacao>('/financial/transactions', transacao);
    return data;
  },

  async atualizar(id: number, transacao: TransacaoInput): Promise<Transacao> {
    const { status: _ignorado, ...corpo } = transacao;
    const { data } = await api.put<Transacao>(`/financial/transactions/${id}`, corpo);
    return data;
  },

  async alterarStatus(id: number, status: TransactionStatus): Promise<Transacao> {
    const { data } = await api.patch<Transacao>(`/financial/transactions/${id}/status`, {
      status,
    });
    return data;
  },

  async excluir(id: number): Promise<void> {
    await api.delete(`/financial/transactions/${id}`);
  },

  /** Resumo do período; sem parâmetros considera o mês corrente (UTC). */
  async resumo(): Promise<ResumoFinanceiro> {
    const { data } = await api.get<ResumoFinanceiro>('/financial/resumo');
    return data;
  },
};
