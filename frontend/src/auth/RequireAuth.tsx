import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

/** Protege um ramo de rotas: sem sessão, redireciona ao login (guardando o destino). */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { autenticado } = useAuth();
  const location = useLocation();

  if (!autenticado) {
    return <Navigate to="/login" replace state={{ de: location.pathname }} />;
  }
  return children;
}
