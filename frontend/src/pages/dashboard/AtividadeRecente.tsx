import GroupAddIcon from '@mui/icons-material/GroupAdd';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { Avatar, Box, Card, CardContent, Skeleton, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import type { ReactElement } from 'react';
import { clientsApi } from '../../api/clients';
import { ordersApi } from '../../api/orders';

interface Evento {
  chave: string;
  texto: string;
  criadoEm: string;
  icone: ReactElement;
  cor: string;
}

/** 'há X minutos/horas/dias' — não é um log de auditoria, só pedidos e clientes recentes. */
function tempoRelativo(iso: string): string {
  const minutos = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 60000));
  if (minutos < 1) return 'agora mesmo';
  if (minutos < 60) return `há ${minutos} minuto${minutos === 1 ? '' : 's'}`;
  const horas = Math.floor(minutos / 60);
  if (horas < 24) return `há ${horas} hora${horas === 1 ? '' : 's'}`;
  const dias = Math.floor(horas / 24);
  return `há ${dias} dia${dias === 1 ? '' : 's'}`;
}

export function AtividadeRecente() {
  const { data: pedidos, isPending: carregandoPedidos } = useQuery({
    queryKey: ['orders', 'atividade-recente'],
    queryFn: () => ordersApi.listar({ page: 0, size: 4 }),
  });

  const { data: clientes, isPending: carregandoClientes } = useQuery({
    queryKey: ['clients', 'atividade-recente'],
    queryFn: () => clientsApi.listar({ page: 0, size: 3, ativo: true, sort: 'criadoEm,desc' }),
  });

  const carregando = carregandoPedidos || carregandoClientes;

  const eventos: Evento[] = [
    ...(pedidos?.content ?? []).map((p) => ({
      chave: `pedido-${p.id}`,
      texto: `Pedido ${p.numero} criado — ${p.clienteNome}`,
      criadoEm: p.criadoEm,
      icone: <ReceiptLongIcon fontSize="small" />,
      cor: '#3987e5',
    })),
    ...(clientes?.content ?? []).map((c) => ({
      chave: `cliente-${c.id}`,
      texto: `Novo cliente cadastrado: ${c.nome}`,
      criadoEm: c.criadoEm,
      icone: <GroupAddIcon fontSize="small" />,
      cor: '#199e70',
    })),
  ]
    .sort((a, b) => b.criadoEm.localeCompare(a.criadoEm))
    .slice(0, 6);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
          Atividade recente
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
          Últimos pedidos e clientes cadastrados
        </Typography>
        {carregando ? (
          <Skeleton variant="rounded" height={200} />
        ) : eventos.length === 0 ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 3, textAlign: 'center' }}>
            Sem atividade registrada ainda.
          </Typography>
        ) : (
          <Stack spacing={1.5}>
            {eventos.map((evento) => (
              <Box key={evento.chave} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Avatar
                  sx={{
                    width: 32,
                    height: 32,
                    bgcolor: (t) => `color-mix(in srgb, ${evento.cor} 22%, ${t.palette.background.paper})`,
                    color: evento.cor,
                  }}
                >
                  {evento.icone}
                </Avatar>
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="body2" noWrap>
                    {evento.texto}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {tempoRelativo(evento.criadoEm)}
                  </Typography>
                </Box>
              </Box>
            ))}
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}
