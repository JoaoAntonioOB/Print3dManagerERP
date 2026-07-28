import PrintIcon from '@mui/icons-material/Print';
import { Avatar, Box, Card, CardContent, Skeleton, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { printsApi } from '../../api/prints';

/** 'há Xh Ymin' desde o início do job — sem inventar % de progresso (não há
 * estimativa de duração confiável ligada ao job em si). */
function tempoDecorrido(iniciadoEm: string, agora: number): string {
  const minutos = Math.max(0, Math.floor((agora - new Date(iniciadoEm).getTime()) / 60000));
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return horas > 0 ? `há ${horas}h ${resto}min` : `há ${resto}min`;
}

export function ProducaoAndamento() {
  const [agora, setAgora] = useState(() => Date.now());

  useEffect(() => {
    const id = setInterval(() => setAgora(Date.now()), 60_000);
    return () => clearInterval(id);
  }, []);

  const { data, isPending } = useQuery({
    queryKey: ['prints', 'painel-andamento'],
    queryFn: () => printsApi.listar({ status: 'EM_ANDAMENTO', page: 0, size: 5 }),
    refetchInterval: 60_000,
  });

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
          Produção em andamento
        </Typography>
        {isPending ? (
          <Skeleton variant="rounded" height={200} />
        ) : !data || data.content.length === 0 ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 3, textAlign: 'center' }}>
            Nenhuma impressão em andamento agora.
          </Typography>
        ) : (
          <Stack spacing={1.5}>
            {data.content.map((job) => (
              <Box key={job.id} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Avatar sx={{ bgcolor: 'secondary.main', width: 36, height: 36 }}>
                  <PrintIcon fontSize="small" />
                </Avatar>
                <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                  <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>
                    {job.nomePeca ?? 'Impressão avulsa'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" noWrap component="div">
                    {job.impressoraNome}
                    {job.pedidoNumero ? ` — ${job.pedidoNumero}` : ''}
                  </Typography>
                </Box>
                <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0 }}>
                  {tempoDecorrido(job.iniciadoEm, agora)}
                </Typography>
              </Box>
            ))}
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}
