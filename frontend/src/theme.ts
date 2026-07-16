import { createTheme } from '@mui/material/styles';
import { ptBR } from '@mui/material/locale';

/** Identidade visual do sistema — mesmo azul-petróleo dos relatórios PDF. */
export const theme = createTheme(
  {
    palette: {
      primary: { main: '#34495e' },
      secondary: { main: '#00897b' },
      background: { default: '#f4f6f8' },
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
      },
    },
  },
  ptBR,
);
