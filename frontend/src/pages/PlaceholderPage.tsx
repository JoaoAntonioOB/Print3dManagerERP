import ConstructionIcon from '@mui/icons-material/Construction';
import { Box, Typography } from '@mui/material';

/** Tela provisória dos módulos ainda não construídos na Etapa 17. */
export function PlaceholderPage({ titulo }: { titulo: string }) {
  return (
    <Box sx={{ textAlign: 'center', mt: 10, color: 'text.secondary' }}>
      <ConstructionIcon sx={{ fontSize: 56, mb: 1 }} />
      <Typography variant="h5" gutterBottom sx={{ fontWeight: 700 }}>
        {titulo}
      </Typography>
      <Typography variant="body1">
        Esta tela será construída nas próximas iterações da Etapa 17 — a API já está pronta.
      </Typography>
    </Box>
  );
}
