import { api } from './client';
import type { OrderStatus } from './orders';

export type TipoRelatorio = 'pedidos' | 'financeiro' | 'consumo-filamento';

export interface ParametrosRelatorio {
  /** ISO (yyyy-MM-dd); omitidos assumem o mês corrente no backend. */
  de?: string;
  ate?: string;
  /** Só no relatório de pedidos. */
  status?: OrderStatus | '';
}

/**
 * Baixa o PDF e dispara o download no browser com o nome vindo do
 * Content-Disposition (exposto no CORS pelo backend).
 */
export async function baixarRelatorio(
  tipo: TipoRelatorio,
  parametros: ParametrosRelatorio,
): Promise<string> {
  const { data, headers } = await api.get<Blob>(`/reports/${tipo}`, {
    responseType: 'blob',
    params: {
      de: parametros.de || undefined,
      ate: parametros.ate || undefined,
      status: parametros.status || undefined,
    },
  });

  const disposition = (headers['content-disposition'] as string | undefined) ?? '';
  const nome =
    /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? `relatorio-${tipo}.pdf`;

  const url = URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  return nome;
}
