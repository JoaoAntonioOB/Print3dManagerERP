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
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { mensagemDeErro } from '../api/client';
import { dashboardApi } from '../api/dashboard';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const compacto = new Intl.NumberFormat('pt-BR', { notation: 'compact' });

// Paleta validada (dataviz): azul para séries únicas/receitas, laranja para despesas.
const AZUL = '#3572b0';
const LARANJA = '#c8611a';
const GRADE = '#e6e6e3';

/** 'YYYY-MM' → 'MM/AA' para o eixo. */
const rotuloMes = (mes: string) => `${mes.slice(5, 7)}/${mes.slice(2, 4)}`;

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

/** Card padrão dos gráficos: título + área de plotagem com altura fixa. */
function CartaoGrafico({ titulo, carregando, children }: {
  titulo: string;
  carregando: boolean;
  children: ReactNode;
}) {
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
          {titulo}
        </Typography>
        {carregando ? (
          <Skeleton variant="rounded" height={260} />
        ) : (
          <Box sx={{ height: 260 }}>{children}</Box>
        )}
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

const MESES_SERIES = 12;

export function DashboardPage() {
  const { data, isPending, error } = useQuery({
    queryKey: ['dashboard', 'resumo'],
    queryFn: () => dashboardApi.resumo(),
  });

  const { data: vendas, isPending: carregandoVendas } = useQuery({
    queryKey: ['dashboard', 'vendas-mensais', MESES_SERIES],
    queryFn: () => dashboardApi.vendasMensais(MESES_SERIES),
  });

  const { data: financeiro, isPending: carregandoFinanceiro } = useQuery({
    queryKey: ['dashboard', 'financeiro-mensal', MESES_SERIES],
    queryFn: () => dashboardApi.financeiroMensal(MESES_SERIES),
  });

  const { data: consumo, isPending: carregandoConsumo } = useQuery({
    queryKey: ['dashboard', 'consumo-filamento', MESES_SERIES],
    queryFn: () => dashboardApi.consumoFilamento(MESES_SERIES),
  });

  const { data: taxa, isPending: carregandoTaxa } = useQuery({
    queryKey: ['dashboard', 'taxa-sucesso'],
    queryFn: () => dashboardApi.taxaSucesso(),
  });

  const { data: topClientes, isPending: carregandoTop } = useQuery({
    queryKey: ['dashboard', 'top-clientes'],
    queryFn: () => dashboardApi.topClientes(5),
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

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          mt: 2,
          gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' },
        }}
      >
        <CartaoGrafico
          titulo={`Faturamento mensal — pedidos entregues (últimos ${MESES_SERIES} meses)`}
          carregando={carregandoVendas}
        >
          <ResponsiveContainer>
            <BarChart data={vendas ?? []} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRADE} vertical={false} />
              <XAxis dataKey="mes" tickFormatter={rotuloMes} tickLine={false} fontSize={12} />
              <YAxis tickFormatter={(v: number) => compacto.format(v)} tickLine={false} fontSize={12} width={48} />
              <Tooltip
                labelFormatter={(mes) => rotuloMes(String(mes))}
                formatter={(valor) => [moeda.format(Number(valor)), 'Faturamento']}
              />
              <Bar dataKey="faturamento" fill={AZUL} radius={[4, 4, 0, 0]} maxBarSize={28} />
            </BarChart>
          </ResponsiveContainer>
        </CartaoGrafico>

        <CartaoGrafico
          titulo={`Pedidos abertos por mês (últimos ${MESES_SERIES} meses)`}
          carregando={carregandoVendas}
        >
          <ResponsiveContainer>
            <LineChart data={vendas ?? []} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRADE} vertical={false} />
              <XAxis dataKey="mes" tickFormatter={rotuloMes} tickLine={false} fontSize={12} />
              <YAxis allowDecimals={false} tickLine={false} fontSize={12} width={32} />
              <Tooltip
                labelFormatter={(mes) => rotuloMes(String(mes))}
                formatter={(valor) => [String(valor), 'Pedidos']}
              />
              <Line
                type="monotone"
                dataKey="pedidos"
                stroke={AZUL}
                strokeWidth={2}
                dot={{ r: 3, fill: AZUL }}
              />
            </LineChart>
          </ResponsiveContainer>
        </CartaoGrafico>

        <CartaoGrafico
          titulo={`Receitas × despesas pagas (últimos ${MESES_SERIES} meses)`}
          carregando={carregandoFinanceiro}
        >
          <ResponsiveContainer>
            <BarChart data={financeiro ?? []} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRADE} vertical={false} />
              <XAxis dataKey="mes" tickFormatter={rotuloMes} tickLine={false} fontSize={12} />
              <YAxis tickFormatter={(v: number) => compacto.format(v)} tickLine={false} fontSize={12} width={48} />
              <Tooltip
                labelFormatter={(mes) => rotuloMes(String(mes))}
                formatter={(valor, nome) => [
                  moeda.format(Number(valor)),
                  nome === 'receitas' ? 'Receitas' : 'Despesas',
                ]}
              />
              <Legend formatter={(nome) => (nome === 'receitas' ? 'Receitas' : 'Despesas')} />
              <Bar dataKey="receitas" fill={AZUL} radius={[4, 4, 0, 0]} maxBarSize={20} />
              <Bar dataKey="despesas" fill={LARANJA} radius={[4, 4, 0, 0]} maxBarSize={20} />
            </BarChart>
          </ResponsiveContainer>
        </CartaoGrafico>

        <CartaoGrafico
          titulo={`Consumo de filamento em gramas (últimos ${MESES_SERIES} meses)`}
          carregando={carregandoConsumo}
        >
          <ResponsiveContainer>
            <BarChart data={consumo ?? []} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
              <CartesianGrid stroke={GRADE} vertical={false} />
              <XAxis dataKey="mes" tickFormatter={rotuloMes} tickLine={false} fontSize={12} />
              <YAxis tickFormatter={(v: number) => compacto.format(v)} tickLine={false} fontSize={12} width={48} />
              <Tooltip
                labelFormatter={(mes) => rotuloMes(String(mes))}
                formatter={(valor) => [`${compacto.format(Number(valor))} g`, 'Consumo']}
              />
              <Bar dataKey="pesoG" fill={AZUL} radius={[4, 4, 0, 0]} maxBarSize={28} />
            </BarChart>
          </ResponsiveContainer>
        </CartaoGrafico>

        <CartaoGrafico titulo="Top 5 clientes por valor de pedidos" carregando={carregandoTop}>
          {topClientes && topClientes.length === 0 ? (
            <Typography color="text.secondary" sx={{ pt: 4, textAlign: 'center' }}>
              Sem pedidos registrados ainda.
            </Typography>
          ) : (
            <ResponsiveContainer>
              <BarChart
                data={topClientes ?? []}
                layout="vertical"
                margin={{ top: 8, right: 24, left: 8, bottom: 0 }}
              >
                <CartesianGrid stroke={GRADE} horizontal={false} />
                <XAxis
                  type="number"
                  tickFormatter={(v: number) => compacto.format(v)}
                  tickLine={false}
                  fontSize={12}
                />
                <YAxis
                  type="category"
                  dataKey="clienteNome"
                  width={140}
                  tickLine={false}
                  fontSize={12}
                />
                <Tooltip
                  formatter={(valor) => [moeda.format(Number(valor)), 'Valor total']}
                />
                <Bar dataKey="valorTotal" fill={AZUL} radius={[0, 4, 4, 0]} maxBarSize={22} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </CartaoGrafico>

        {/* Taxa de sucesso: número-manchete, não gráfico (um único valor). */}
        <Card>
          <CardContent>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
              Taxa de sucesso das impressões
            </Typography>
            {carregandoTaxa || !taxa ? (
              <Skeleton variant="rounded" height={260} />
            ) : (
              <Box
                sx={{
                  height: 260,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 2,
                }}
              >
                <Typography variant="h2" color="primary" sx={{ fontWeight: 700 }}>
                  {taxa.taxaSucesso != null ? `${String(taxa.taxaSucesso).replace('.', ',')}%` : '—'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {taxa.taxaSucesso != null
                    ? 'das impressões finalizadas foram concluídas com sucesso'
                    : 'Nenhuma impressão finalizada ainda'}
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, justifyContent: 'center' }}>
                  <Chip size="small" color="success" label={`Concluídas: ${taxa.concluidas}`} />
                  <Chip size="small" color="error" label={`Falhas: ${taxa.falhas}`} />
                  <Chip size="small" label={`Canceladas: ${taxa.canceladas}`} />
                  <Chip size="small" color="info" label={`Em andamento: ${taxa.emAndamento}`} />
                </Box>
              </Box>
            )}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
