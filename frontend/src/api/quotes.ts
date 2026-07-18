import { api } from './client';
import type { PageResponse } from './types';

export type QuoteStatus =
  | 'RASCUNHO'
  | 'ENVIADO'
  | 'APROVADO'
  | 'REJEITADO'
  | 'EXPIRADO'
  | 'CONVERTIDO';

export const ROTULOS_STATUS_ORCAMENTO: Record<QuoteStatus, string> = {
  RASCUNHO: 'Rascunho',
  ENVIADO: 'Enviado',
  APROVADO: 'Aprovado',
  REJEITADO: 'Rejeitado',
  EXPIRADO: 'Expirado',
  CONVERTIDO: 'Convertido',
};

export const CORES_STATUS_ORCAMENTO: Record<
  QuoteStatus,
  'default' | 'info' | 'success' | 'error' | 'warning' | 'secondary'
> = {
  RASCUNHO: 'default',
  ENVIADO: 'info',
  APROVADO: 'success',
  REJEITADO: 'error',
  EXPIRADO: 'warning',
  CONVERTIDO: 'secondary',
};

/** Transições permitidas via PATCH — CONVERTIDO só existe pela conversão em pedido. */
export const TRANSICOES_ORCAMENTO: Record<QuoteStatus, QuoteStatus[]> = {
  RASCUNHO: ['ENVIADO'],
  ENVIADO: ['RASCUNHO', 'APROVADO', 'REJEITADO', 'EXPIRADO'],
  APROVADO: [],
  REJEITADO: [],
  EXPIRADO: [],
  CONVERTIDO: [],
};

export interface Orcamento {
  id: number;
  numero: string;
  clienteId: number;
  clienteNome: string;
  usuarioId: number;
  usuarioNome: string;
  impressoraId: number | null;
  impressoraNome: string | null;
  filamentoId: number | null;
  filamentoNome: string | null;
  pedidoId: number | null;
  pedidoNumero: string | null;
  status: QuoteStatus;
  shareToken: string;
  descricao: string | null;
  dataValidade: string | null;
  tempoImpressaoMinutos: number | null;
  pesoEstimadoG: number | null;
  custoFilamento: number;
  custoEnergia: number;
  custoHoraMaquina: number;
  custoDesgaste: number;
  custoTotal: number;
  markup: number;
  precoSugerido: number;
  precoFinal: number | null;
  precoEfetivo: number;
  aprovadoEm: string | null;
  observacoes: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface OrcamentoInput {
  clienteId: number;
  impressoraId: number | null;
  filamentoId: number | null;
  descricao: string | null;
  dataValidade: string | null;
  tempoImpressaoMinutos: number | null;
  pesoEstimadoG: number | null;
  /** Omitido no cadastro usa o markup padrão da configuração efetiva da impressora. */
  markup: number | null;
  precoFinal: number | null;
  observacoes: string | null;
}

export interface FiltrosOrcamentos {
  busca?: string;
  status?: QuoteStatus | '';
  page: number;
  size: number;
}

/** Visão do cliente pelo link público — sem custos internos nem markup. */
export interface OrcamentoPublico {
  numero: string;
  clienteNome: string;
  descricao: string | null;
  status: QuoteStatus;
  dataValidade: string | null;
  tempoImpressaoMinutos: number | null;
  pesoEstimadoG: number | null;
  preco: number;
  aprovadoEm: string | null;
  criadoEm: string;
}

/** Pedido devolvido pela conversão (campos usados na tela). */
export interface PedidoConvertido {
  id: number;
  numero: string;
}

export const quotesApi = {
  async listar(filtros: FiltrosOrcamentos): Promise<PageResponse<Orcamento>> {
    const { data } = await api.get<PageResponse<Orcamento>>('/quotes', {
      params: {
        busca: filtros.busca || undefined,
        status: filtros.status || undefined,
        page: filtros.page,
        size: filtros.size,
      },
    });
    return data;
  },

  async buscarPorId(id: number): Promise<Orcamento> {
    const { data } = await api.get<Orcamento>(`/quotes/${id}`);
    return data;
  },

  async criar(orcamento: OrcamentoInput): Promise<Orcamento> {
    const { data } = await api.post<Orcamento>('/quotes', orcamento);
    return data;
  },

  async atualizar(id: number, orcamento: OrcamentoInput): Promise<Orcamento> {
    const { data } = await api.put<Orcamento>(`/quotes/${id}`, orcamento);
    return data;
  },

  async alterarStatus(id: number, status: QuoteStatus): Promise<Orcamento> {
    const { data } = await api.patch<Orcamento>(`/quotes/${id}/status`, { status });
    return data;
  },

  async converter(id: number): Promise<PedidoConvertido> {
    const { data } = await api.post<PedidoConvertido>(`/quotes/${id}/converter`);
    return data;
  },

  async excluir(id: number): Promise<void> {
    await api.delete(`/quotes/${id}`);
  },

  async buscarPublico(shareToken: string): Promise<OrcamentoPublico> {
    const { data } = await api.get<OrcamentoPublico>(`/public/quotes/${shareToken}`);
    return data;
  },

  async aprovarPublico(shareToken: string): Promise<OrcamentoPublico> {
    const { data } = await api.post<OrcamentoPublico>(`/public/quotes/${shareToken}/aprovar`);
    return data;
  },

  async recusarPublico(shareToken: string): Promise<OrcamentoPublico> {
    const { data } = await api.post<OrcamentoPublico>(`/public/quotes/${shareToken}/recusar`);
    return data;
  },
};
