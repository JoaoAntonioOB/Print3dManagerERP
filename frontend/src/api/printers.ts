import axios from 'axios';
import { api } from './client';
import type { PageResponse } from './types';

export type PrinterStatus = 'DISPONIVEL' | 'IMPRIMINDO' | 'EM_MANUTENCAO' | 'INATIVA';

export interface Impressora {
  id: number;
  nome: string;
  marca: string | null;
  modelo: string | null;
  status: PrinterStatus;
  potenciaWatts: number | null;
  volumeXMm: number | null;
  volumeYMm: number | null;
  volumeZMm: number | null;
  horasImpressaoTotal: number;
  valorAquisicao: number | null;
  dataAquisicao: string | null;
  observacoes: string | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ImpressoraInput {
  nome: string;
  marca: string | null;
  modelo: string | null;
  potenciaWatts: number | null;
  volumeXMm: number | null;
  volumeYMm: number | null;
  volumeZMm: number | null;
  valorAquisicao: number | null;
  dataAquisicao: string | null;
  observacoes: string | null;
}

export interface ConfiguracaoCusto {
  id: number;
  impressoraId: number | null;
  valorKwh: number | null;
  valorHoraMaquina: number | null;
  custoDesgasteHora: number | null;
  markupPadrao: number | null;
  origem?: 'PROPRIA' | 'GLOBAL' | null;
}

export interface ConfiguracaoCustoInput {
  valorKwh: number | null;
  valorHoraMaquina: number | null;
  custoDesgasteHora: number | null;
  markupPadrao: number | null;
}

export interface FiltrosImpressoras {
  busca?: string;
  status?: PrinterStatus | '';
  ativo?: boolean | '';
  page: number;
  size: number;
}

export const printersApi = {
  async listar(filtros: FiltrosImpressoras): Promise<PageResponse<Impressora>> {
    const { data } = await api.get<PageResponse<Impressora>>('/printers', {
      params: {
        busca: filtros.busca || undefined,
        status: filtros.status || undefined,
        ativo: filtros.ativo === '' ? undefined : filtros.ativo,
        page: filtros.page,
        size: filtros.size,
        sort: 'nome,asc',
      },
    });
    return data;
  },

  async criar(impressora: ImpressoraInput): Promise<Impressora> {
    const { data } = await api.post<Impressora>('/printers', impressora);
    return data;
  },

  async atualizar(id: number, impressora: ImpressoraInput): Promise<Impressora> {
    const { data } = await api.put<Impressora>(`/printers/${id}`, impressora);
    return data;
  },

  async alterarStatus(id: number, status: PrinterStatus): Promise<Impressora> {
    const { data } = await api.patch<Impressora>(`/printers/${id}/status`, { status });
    return data;
  },

  async desativar(id: number): Promise<void> {
    await api.delete(`/printers/${id}`);
  },

  async reativar(id: number): Promise<Impressora> {
    const { data } = await api.patch<Impressora>(`/printers/${id}/ativar`);
    return data;
  },

  /** Configuração global de custos; null quando ainda não cadastrada (404). */
  async obterConfigGlobal(): Promise<ConfiguracaoCusto | null> {
    try {
      const { data } = await api.get<ConfiguracaoCusto>('/printers/config');
      return data;
    } catch (erro) {
      if (axios.isAxiosError(erro) && erro.response?.status === 404) {
        return null;
      }
      throw erro;
    }
  },

  async salvarConfigGlobal(config: ConfiguracaoCustoInput): Promise<ConfiguracaoCusto> {
    const { data } = await api.put<ConfiguracaoCusto>('/printers/config', config);
    return data;
  },

  /** Configuração efetiva da impressora (própria ou global); null sem nenhuma (404). */
  async obterConfigEfetiva(impressoraId: number): Promise<ConfiguracaoCusto | null> {
    try {
      const { data } = await api.get<ConfiguracaoCusto>(`/printers/${impressoraId}/config`);
      return data;
    } catch (erro) {
      if (axios.isAxiosError(erro) && erro.response?.status === 404) {
        return null;
      }
      throw erro;
    }
  },

  async salvarConfigPropria(
    impressoraId: number,
    config: ConfiguracaoCustoInput,
  ): Promise<ConfiguracaoCusto> {
    const { data } = await api.put<ConfiguracaoCusto>(
      `/printers/${impressoraId}/config`,
      config,
    );
    return data;
  },

  async removerConfigPropria(impressoraId: number): Promise<void> {
    await api.delete(`/printers/${impressoraId}/config`);
  },
};
