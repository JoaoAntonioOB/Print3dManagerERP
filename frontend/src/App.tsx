import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './layout/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { PlaceholderPage } from './pages/PlaceholderPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="pedidos" element={<PlaceholderPage titulo="Pedidos" />} />
        <Route path="orcamentos" element={<PlaceholderPage titulo="Orçamentos" />} />
        <Route path="clientes" element={<PlaceholderPage titulo="Clientes" />} />
        <Route path="filamentos" element={<PlaceholderPage titulo="Filamentos" />} />
        <Route path="estoque" element={<PlaceholderPage titulo="Estoque" />} />
        <Route path="impressoras" element={<PlaceholderPage titulo="Impressoras" />} />
        <Route path="impressoes" element={<PlaceholderPage titulo="Impressões" />} />
        <Route path="financeiro" element={<PlaceholderPage titulo="Financeiro" />} />
        <Route path="relatorios" element={<PlaceholderPage titulo="Relatórios" />} />
        <Route path="usuarios" element={<PlaceholderPage titulo="Usuários" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
