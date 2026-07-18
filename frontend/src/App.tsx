import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { ClientsPage } from './pages/clients/ClientsPage';
import { FilamentsPage } from './pages/filaments/FilamentsPage';
import { FinancialPage } from './pages/financial/FinancialPage';
import { InventoryPage } from './pages/inventory/InventoryPage';
import { OrdersPage } from './pages/orders/OrdersPage';
import { PrintersPage } from './pages/printers/PrintersPage';
import { PrintsPage } from './pages/prints/PrintsPage';
import { PublicQuotePage } from './pages/quotes/PublicQuotePage';
import { QuotesPage } from './pages/quotes/QuotesPage';
import { AppLayout } from './layout/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { PlaceholderPage } from './pages/PlaceholderPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/orcamento/:shareToken" element={<PublicQuotePage />} />
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="pedidos" element={<OrdersPage />} />
        <Route path="orcamentos" element={<QuotesPage />} />
        <Route path="clientes" element={<ClientsPage />} />
        <Route path="filamentos" element={<FilamentsPage />} />
        <Route path="estoque" element={<InventoryPage />} />
        <Route path="impressoras" element={<PrintersPage />} />
        <Route path="impressoes" element={<PrintsPage />} />
        <Route path="financeiro" element={<FinancialPage />} />
        <Route path="relatorios" element={<PlaceholderPage titulo="Relatórios" />} />
        <Route path="usuarios" element={<PlaceholderPage titulo="Usuários" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
