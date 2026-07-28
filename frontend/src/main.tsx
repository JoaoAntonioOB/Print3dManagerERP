import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Toaster } from 'react-hot-toast';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { AuthProvider } from './auth/AuthContext';
import { theme } from './theme';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#1a222c',
            color: '#e7edf3',
            border: '1px solid rgba(231, 237, 243, 0.12)',
          },
          success: {
            iconTheme: { primary: '#2bbbad', secondary: '#0b1e1b' },
          },
          error: {
            iconTheme: { primary: '#f2867e', secondary: '#2a1210' },
          },
        }}
      />
    </ThemeProvider>
  </StrictMode>,
);
