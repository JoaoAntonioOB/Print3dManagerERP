import { useEffect, useState } from 'react';

/** Devolve o valor só depois de ficar estável por {@code atrasoMs} — busca conforme digita. */
export function useDebounce<T>(valor: T, atrasoMs = 400): T {
  const [debounced, setDebounced] = useState(valor);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(valor), atrasoMs);
    return () => clearTimeout(timer);
  }, [valor, atrasoMs]);

  return debounced;
}
