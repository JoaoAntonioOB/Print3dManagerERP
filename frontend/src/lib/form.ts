import { z } from 'zod';

/**
 * Helpers de formulário: campos numéricos são editados como texto (aceitando
 * vírgula ou ponto decimal, como o usuário brasileiro digita) e convertidos
 * na montagem do payload.
 */

const PADRAO_DECIMAL = /^\d+([.,]\d+)?$/;
const PADRAO_INTEIRO = /^\d+$/;

export const decimalOpcional = (mensagem = 'Número inválido (use 0,00).') =>
  z.union([z.literal(''), z.string().trim().regex(PADRAO_DECIMAL, mensagem)]);

export const decimalObrigatorio = (mensagem = 'Número inválido (use 0,00).') =>
  z.string().trim().min(1, 'Campo obrigatório.').regex(PADRAO_DECIMAL, mensagem);

export const inteiroOpcional = (mensagem = 'Informe um número inteiro.') =>
  z.union([z.literal(''), z.string().trim().regex(PADRAO_INTEIRO, mensagem)]);

/** '12,5' → 12.5; '' → null. */
export function paraNumero(valor: string): number | null {
  const texto = valor.trim();
  return texto === '' ? null : Number(texto.replace(',', '.'));
}

/** '' → null; demais valores com trim. */
export function paraTexto(valor: string): string | null {
  const texto = valor.trim();
  return texto === '' ? null : texto;
}

/** Formata números para exibição/edição em pt-BR (ponto → vírgula). */
export function paraCampo(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? '' : String(valor).replace('.', ',');
}
