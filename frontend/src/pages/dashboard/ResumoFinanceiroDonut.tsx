import { Box, Card, CardContent, Skeleton, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { financialApi } from '../../api/financial';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

const VERDE = '#0ca30c';
const LARANJA = '#d95926';

const TOOLTIP_ESTILO = {
  contentStyle: {
    background: '#1a222c',
    border: '1px solid rgba(231, 237, 243, 0.12)',
    borderRadius: 8,
    color: '#e7edf3',
  },
  itemStyle: { color: '#e7edf3' },
};

/** Legenda com quadradinho de cor — Pie não expõe Legend com cor certa fora da caixa. */
function ItemLegenda({ cor, rotulo, valor }: { cor: string; rotulo: string; valor: number }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: cor, flexShrink: 0 }} />
      <Typography variant="body2" sx={{ flexGrow: 1 }}>
        {rotulo}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: 600 }}>
        {moeda.format(valor)}
      </Typography>
    </Box>
  );
}

export function ResumoFinanceiroDonut() {
  const { data, isPending } = useQuery({
    queryKey: ['financial', 'resumo-painel'],
    queryFn: () => financialApi.resumo(),
  });

  const dados = data
    ? [
        { nome: 'Receitas', valor: data.receitasPagas, cor: VERDE },
        { nome: 'Despesas', valor: data.despesasPagas, cor: LARANJA },
      ]
    : [];
  const semDados = data && data.receitasPagas === 0 && data.despesasPagas === 0;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
          Resumo financeiro (mês)
        </Typography>
        {isPending || !data ? (
          <Skeleton variant="rounded" height={220} />
        ) : semDados ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 3, textAlign: 'center' }}>
            Nenhuma transação paga neste mês.
          </Typography>
        ) : (
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: 'center' }}>
            <Box sx={{ width: 140, height: 140, position: 'relative', flexShrink: 0 }}>
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={dados}
                    dataKey="valor"
                    nameKey="nome"
                    innerRadius={42}
                    outerRadius={64}
                    paddingAngle={2}
                    isAnimationActive={false}
                  >
                    {dados.map((d) => (
                      <Cell key={d.nome} fill={d.cor} stroke="none" />
                    ))}
                  </Pie>
                  <Tooltip {...TOOLTIP_ESTILO} formatter={(v) => moeda.format(Number(v))} />
                </PieChart>
              </ResponsiveContainer>
              <Box
                sx={{
                  position: 'absolute',
                  inset: 0,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  pointerEvents: 'none',
                }}
              >
                <Typography variant="caption" color="text.secondary">
                  Saldo
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                  {moeda.format(data.saldoRealizado)}
                </Typography>
              </Box>
            </Box>
            <Stack spacing={1} sx={{ flexGrow: 1, width: '100%' }}>
              <ItemLegenda cor={VERDE} rotulo="Receitas pagas" valor={data.receitasPagas} />
              <ItemLegenda cor={LARANJA} rotulo="Despesas pagas" valor={data.despesasPagas} />
            </Stack>
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}
