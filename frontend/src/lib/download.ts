/** Extrai o nome de arquivo do header Content-Disposition (exposto no CORS). */
export function nomeDoContentDisposition(
  disposition: string | undefined,
  fallback: string,
): string {
  return /filename="?([^";]+)"?/.exec(disposition ?? '')?.[1] ?? fallback;
}

/** Dispara o download de um Blob no browser com o nome informado. */
export function dispararDownloadBlob(conteudo: Blob, nome: string): void {
  const url = URL.createObjectURL(conteudo);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
