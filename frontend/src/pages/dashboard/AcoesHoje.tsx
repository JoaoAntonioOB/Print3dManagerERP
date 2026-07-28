import InventoryIcon from '@mui/icons-material/Inventory2';
import PrintIcon from '@mui/icons-material/Print';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { Box, Paper, Typography } from '@mui/material';
import type { ReactElement } from 'react';
import { useNavigate } from 'react-router-dom';
import type { DashboardResumo } from '../../api/types';

interface CardAcao {
  titulo: string;
  quantidade: number;
  descricao: string;
  icone: ReactElement;
  cor: string;
  rota: string;
}

function CartaoAcao({ titulo, quantidade, descricao, icone, cor, rota }: CardAcao) {
  const navigate = useNavigate();
  const ativo = quantidade > 0;

  return (
    <Paper
      onClick={() => navigate(rota)}
      sx={{
        p: 2,
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        cursor: 'pointer',
        border: '1px solid',
        borderColor: ativo ? cor : 'divider',
        transition: 'border-color 0.15s, background-color 0.15s',
        '&:hover': { bgcolor: 'action.hover' },
      }}
    >
      <Box
        sx={{
          width: 40,
          height: 40,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: (t) => `color-mix(in srgb, ${cor} 20%, ${t.palette.background.paper})`,
          color: cor,
          flexShrink: 0,
        }}
      >
        {icone}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="body2" sx={{ fontWeight: 700, lineHeight: 1.3 }}>
          {quantidade} {titulo}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {descricao}
        </Typography>
      </Box>
    </Paper>
  );
}

interface AcoesHojeProps {
  resumo: DashboardResumo;
}

/**
 * Faixa de alertas do dia — só com dados reais que o resumo já traz. O
 * mockup de referência também mostrava "impressões atrasadas/paradas", mas
 * não existe conceito de prazo por impressão no sistema, então não entra.
 */
export function AcoesHoje({ resumo }: AcoesHojeProps) {
  const estoqueBaixo = resumo.filamentosEstoqueBaixo + resumo.itensEstoqueBaixo;

  const cards: CardAcao[] = [
    {
      titulo: 'pedidos aguardando produção',
      quantidade: resumo.pedidosPorStatus.PENDENTE ?? 0,
      descricao: 'Ver pedidos',
      icone: <ReceiptLongIcon fontSize="small" />,
      cor: '#3987e5',
      rota: '/pedidos',
    },
    {
      titulo: 'impressões em andamento',
      quantidade: resumo.impressoesEmAndamento,
      descricao: 'Ver impressões',
      icone: <PrintIcon fontSize="small" />,
      cor: '#199e70',
      rota: '/impressoes',
    },
    {
      titulo: 'itens com estoque baixo',
      quantidade: estoqueBaixo,
      descricao: 'Ver estoque',
      icone: <InventoryIcon fontSize="small" />,
      cor: '#fab219',
      rota: '/filamentos',
    },
  ];

  return (
    <Box
      sx={{
        display: 'grid',
        gap: 2,
        gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
        mb: 2,
      }}
    >
      {cards.map((card) => (
        <CartaoAcao key={card.titulo} {...card} />
      ))}
    </Box>
  );
}
