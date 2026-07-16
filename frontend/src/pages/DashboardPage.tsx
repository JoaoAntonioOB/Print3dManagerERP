import GroupIcon from '@mui/icons-material/Group';
import PaidIcon from '@mui/icons-material/Paid';
import PrintIcon from '@mui/icons-material/Print';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import type { ReactElement, ReactNode } from 'react';
import { api, mensagemDeErro } from '../api/client';
import type { DashboardResumo } from '../api/types';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function CartaoIndicador({
  titulo,
  valor,
  icone,
  rodape,
}: {
  titulo: string;
  valor: ReactNode;
  icone: ReactElement;
  rodape?: ReactNode;
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
          <Box sx={{ color: 'primary.main', display: 'flex' }}>{icone}</Box>
          <Typography variant="body2" color="text.secondary">
            {titulo}
          </Typography>
        </Stack>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          {valor}
        </Typography>
        {rodape && <Box sx={{ mt: 1 }}>{rodape}</Box>}
      </CardContent>
    </Card>
  );
}

const ROTULOS_STATUS_PEDIDO: Record<string, string> = {
  PENDENTE: 'Pendentes',
  EM_PRODUCAO: 'Em produção',
  CONCLUIDO: 'Concluídos',
  ENTREGUE: 'Entregues',
  CANCELADO: 'Cancelados',
};

export function DashboardPage() {
  const { data, isPending, error } = useQuery({
    queryKey: ['dashboard', 'resumo'],
    queryFn: async () => (await api.get<DashboardResumo>('/dashboard/resumo')).data,
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Dashboard
      </Typography>

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(4, 1fr)' },
        }}
      >
        {isPending || !data ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} variant="rounded" height={140} />
          ))
        ) : (
          <>
            <CartaoIndicador
              titulo="Pedidos no mês"
              valor={data.pedidosMesAtual}
              icone={<ReceiptLongIcon />}
              rodape={
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {Object.entries(data.pedidosPorStatus)
                    .filter(([, quantidade]) => quantidade > 0)
                    .map(([status, quantidade]) => (
                      <Chip
                        key={status}
                        size="small"
                        label={`${ROTULOS_STATUS_PEDIDO[status] ?? status}: ${quantidade}`}
                      />
                    ))}
                </Box>
              }
            />
            <CartaoIndicador
              titulo="Faturamento no mês (entregues)"
              valor={moeda.format(data.faturamentoMesAtual)}
              icone={<PaidIcon />}
            />
            <CartaoIndicador
              titulo="Impressões em andamento"
              valor={data.impressoesEmAndamento}
              icone={<PrintIcon />}
            />
            <CartaoIndicador
              titulo="Clientes ativos"
              valor={data.clientesAtivos}
              icone={<GroupIcon />}
              rodape={
                data.filamentosEstoqueBaixo + data.itensEstoqueBaixo > 0 ? (
                  <Chip
                    size="small"
                    color="warning"
                    icon={<ReportProblemIcon />}
                    label={`Estoque baixo: ${data.filamentosEstoqueBaixo} filamento(s), ${data.itensEstoqueBaixo} insumo(s)`}
                  />
                ) : undefined
              }
            />
          </>
        )}
      </Box>

      <Alert severity="info" sx={{ mt: 3 }}>
        Gráficos de vendas, consumo de filamento e financeiro entram nas próximas telas da
        Etapa 17.
      </Alert>
    </Box>
  );
}
