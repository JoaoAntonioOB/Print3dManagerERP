import { createTheme } from '@mui/material/styles';
import { ptBR } from '@mui/material/locale';

/**
 * Identidade visual do sistema em modo escuro — índigo/violeta como cor de
 * marca (referência visual pedida pelo usuário), com o verde-água como
 * secundária. Tons validados com a skill dataviz (script validate_palette.js)
 * contra o fundo escuro do app: passam o piso de contraste às cegas para cor
 * (CVD) e o piso de contraste normal — por isso o texto sobre os dois vira
 * escuro, não branco (branco sobre o violeta claro mede só 3.13:1).
 */
export const theme = createTheme(
  {
    palette: {
      mode: 'dark',
      primary: {
        main: '#9085e9',
        light: '#b3ace7',
        dark: '#4a3aa7',
        contrastText: '#14101f',
      },
      secondary: {
        main: '#199e70',
        light: '#4fbb95',
        dark: '#0f7355',
        contrastText: '#04140f',
      },
      background: {
        default: '#111820',
        paper: '#1a222c',
      },
      text: {
        primary: '#e7edf3',
        secondary: '#9fb0c0',
      },
      divider: 'rgba(231, 237, 243, 0.12)',
    },
    shape: {
      borderRadius: 10,
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
