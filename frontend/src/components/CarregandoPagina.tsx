import { Box, CircularProgress } from '@mui/material';

/** Fallback simples de carregamento para páginas lazy-loaded (React.lazy/Suspense). */
export function CarregandoPagina() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
      <CircularProgress />
    </Box>
  );
}
