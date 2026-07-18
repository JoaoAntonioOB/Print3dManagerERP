import { api } from './client';
import type { DashboardResumo } from './types';

/** Ponto mensal de vendas: pedidos abertos e faturamento dos entregues. */
export interface PontoVendasMensais {
  mes: string;
  pedidos: number;
  faturamento: number;
}

export interface PontoConsumoFilamento {
  mes: string;
  pesoG: number;
}

export interface TaxaSucessoImpressoes {
  concluidas: number;
  falhas: number;
  canceladas: number;
  emAndamento: number;
  /** Percentual CONCLUIDA ÷ (CONCLUIDA+FALHOU); null sem impressões finalizadas. */
  taxaSucesso: number | null;
}

export interface TopCliente {
  clienteId: number;
  clienteNome: string;
  pedidos: number;
  valorTotal: number;
}

/** Ponto mensal do financeiro realizado (transações PAGAS). */
export interface PontoFinanceiroMensal {
  mes: string;
  receitas: number;
  despesas: number;
  saldo: number;
}

export const dashboardApi = {
  async resumo(): Promise<DashboardResumo> {
    const { data } = await api.get<DashboardResumo>('/dashboard/resumo');
    return data;
  },

  async vendasMensais(meses: number): Promise<PontoVendasMensais[]> {
    const { data } = await api.get<PontoVendasMensais[]>('/dashboard/vendas-mensais', {
      params: { meses },
    });
    return data;
  },

  async consumoFilamento(meses: number): Promise<PontoConsumoFilamento[]> {
    const { data } = await api.get<PontoConsumoFilamento[]>('/dashboard/consumo-filamento', {
      params: { meses },
    });
    return data;
  },

  async taxaSucesso(): Promise<TaxaSucessoImpressoes> {
    const { data } = await api.get<TaxaSucessoImpressoes>('/dashboard/taxa-sucesso');
    return data;
  },

  async topClientes(limite: number): Promise<TopCliente[]> {
    const { data } = await api.get<TopCliente[]>('/dashboard/top-clientes', {
      params: { limite },
    });
    return data;
  },

  async financeiroMensal(meses: number): Promise<PontoFinanceiroMensal[]> {
    const { data } = await api.get<PontoFinanceiroMensal[]>('/financial/resumo/mensal', {
      params: { meses },
    });
    return data;
  },
};
