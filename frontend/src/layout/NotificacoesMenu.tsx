import NotificationsIcon from '@mui/icons-material/Notifications';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import PrintIcon from '@mui/icons-material/Print';
import { Badge, Box, IconButton, Menu, MenuItem, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState, type ReactElement } from 'react';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '../api/dashboard';

interface Alerta {
  texto: string;
  icone: ReactElement;
  rota: string;
}

/** Sino de alertas — reaproveita o mesmo resumo do Dashboard (cache compartilhado). */
export function NotificacoesMenu() {
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const { data } = useQuery({
    queryKey: ['dashboard', 'resumo'],
    queryFn: () => dashboardApi.resumo(),
  });

  const alertas: Alerta[] = [];
  if (data) {
    const pendentes = data.pedidosPorStatus.PENDENTE ?? 0;
    const estoqueBaixo = data.filamentosEstoqueBaixo + data.itensEstoqueBaixo;
    if (pendentes > 0) {
      alertas.push({
        texto: `${pendentes} pedido(s) aguardando produção`,
        icone: <ReceiptLongIcon fontSize="small" color="info" />,
        rota: '/pedidos',
      });
    }
    if (data.impressoesEmAndamento > 0) {
      alertas.push({
        texto: `${data.impressoesEmAndamento} impressão(ões) em andamento`,
        icone: <PrintIcon fontSize="small" color="success" />,
        rota: '/impressoes',
      });
    }
    if (estoqueBaixo > 0) {
      alertas.push({
        texto: `${estoqueBaixo} item(ns) com estoque baixo`,
        icone: <ReportProblemIcon fontSize="small" color="warning" />,
        rota: '/filamentos',
      });
    }
  }

  return (
    <>
      <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)} aria-label="Notificações">
        <Badge badgeContent={alertas.length} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>
      <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
        {alertas.length === 0 ? (
          <Box sx={{ px: 2, py: 1.5, maxWidth: 260 }}>
            <Typography variant="body2" color="text.secondary">
              Nenhum alerta no momento.
            </Typography>
          </Box>
        ) : (
          alertas.map((alerta) => (
            <MenuItem
              key={alerta.texto}
              onClick={() => {
                navigate(alerta.rota);
                setAnchorEl(null);
              }}
              sx={{ gap: 1.5, maxWidth: 300 }}
            >
              {alerta.icone}
              <Typography variant="body2">{alerta.texto}</Typography>
            </MenuItem>
          ))
        )}
      </Menu>
    </>
  );
}
