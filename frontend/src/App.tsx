import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { CarregandoPagina } from './components/CarregandoPagina';
import { AppLayout } from './layout/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';

// Code-splitting por página: só a tela inicial (dashboard) e o login entram
// no chunk principal — o resto é carregado sob demanda (ver Suspense em AppLayout).
const OrdersPage = lazy(() => import('./pages/orders/OrdersPage').then((m) => ({ default: m.OrdersPage })));
const QuotesPage = lazy(() => import('./pages/quotes/QuotesPage').then((m) => ({ default: m.QuotesPage })));
const ClientsPage = lazy(() => import('./pages/clients/ClientsPage').then((m) => ({ default: m.ClientsPage })));
const FilamentsPage = lazy(() =>
  import('./pages/filaments/FilamentsPage').then((m) => ({ default: m.FilamentsPage })),
);
const InventoryPage = lazy(() =>
  import('./pages/inventory/InventoryPage').then((m) => ({ default: m.InventoryPage })),
);
const PrintersPage = lazy(() =>
  import('./pages/printers/PrintersPage').then((m) => ({ default: m.PrintersPage })),
);
const PrintsPage = lazy(() => import('./pages/prints/PrintsPage').then((m) => ({ default: m.PrintsPage })));
const FinancialPage = lazy(() =>
  import('./pages/financial/FinancialPage').then((m) => ({ default: m.FinancialPage })),
);
const ReportsPage = lazy(() => import('./pages/reports/ReportsPage').then((m) => ({ default: m.ReportsPage })));
const UsersPage = lazy(() => import('./pages/users/UsersPage').then((m) => ({ default: m.UsersPage })));
const PublicQuotePage = lazy(() =>
  import('./pages/quotes/PublicQuotePage').then((m) => ({ default: m.PublicQuotePage })),
);

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/orcamento/:shareToken"
        element={
          <Suspense fallback={<CarregandoPagina />}>
            <PublicQuotePage />
          </Suspense>
        }
      />
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
        <Route path="relatorios" element={<ReportsPage />} />
        <Route path="usuarios" element={<UsersPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
