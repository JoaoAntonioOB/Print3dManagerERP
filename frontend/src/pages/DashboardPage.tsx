import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
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
  PolarAngleAxis,
  RadialBar,
  RadialBarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { mensagemDeErro } from '../api/client';
import { dashboardApi } from '../api/dashboard';
import { AcoesHoje } from './dashboard/AcoesHoje';
import { AtividadeRecente } from './dashboard/AtividadeRecente';
import { EstoqueCritico } from './dashboard/EstoqueCritico';
import { ProducaoAndamento } from './dashboard/ProducaoAndamento';
import { ResumoFinanceiroDonut } from './dashboard/ResumoFinanceiroDonut';
import { Sparkline } from './dashboard/Sparkline';

/** % de variação entre os dois últimos pontos de uma série mensal (null sem base suficiente). */
function variacaoPercentual(serie: number[]): number | null {
  if (serie.length < 2) return null;
  const atual = serie[serie.length - 1];
  const anterior = serie[serie.length - 2];
  if (anterior === 0) return atual > 0 ? 100 : null;
  return ((atual - anterior) / anterior) * 100;
}

function ChipVariacao({ variacao }: { variacao: number }) {
  const positiva = variacao >= 0;
  return (
    <Chip
      size="small"
      color={positiva ? 'success' : 'error'}
      icon={positiva ? <ArrowUpwardIcon /> : <ArrowDownwardIcon />}
      label={`${positiva ? '+' : ''}${variacao.toFixed(0)}% vs mês anterior`}
    />
  );
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const compacto = new Intl.NumberFormat('pt-BR', { notation: 'compact' });

// Paleta validada (dataviz, contra o fundo escuro do app): azul para séries
// únicas/receitas, laranja para despesas — mesmo par usado antes, hex
// atualizado para os steps validados em modo escuro (CVD + contraste).
const AZUL = '#3987e5';
const LARANJA = '#d95926';
const GRADE = 'rgba(231, 237, 243, 0.16)';

// Chrome do tooltip (fundo/borda/texto) casado com o tema escuro — não mexe na
// paleta de série (AZUL/LARANJA), que fica para revisão futura com a skill dataviz.
const TOOLTIP_ESTILO = {
  contentStyle: {
    background: '#1a222c',
    border: '1px solid rgba(231, 237, 243, 0.12)',
    borderRadius: 8,
    color: '#e7edf3',
  },
  labelStyle: { color: '#9fb0c0' },
  itemStyle: { color: '#e7edf3' },
};

/** 'YYYY-MM' → 'MM/AA' para o eixo. */
const rotuloMes = (mes: string) => `${mes.slice(5, 7)}/${mes.slice(2, 4)}`;

function CartaoIndicador({
  titulo,
  valor,
  icone,
  cor = '#3987e5',
  variacao,
  sparklineDados,
  rodape,
}: {
  titulo: string;
  valor: ReactNode;
  icone: ReactElement;
  cor?: string;
  variacao?: number | null;
  sparklineDados?: number[];
  rodape?: ReactNode;
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1.5 }}>
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: (t) => `color-mix(in srgb, ${cor} 20%, ${t.palette.background.paper})`,
              color: cor,
            }}
          >
            {icone}
          </Box>
          <Typography variant="body2" color="text.secondary">
            {titulo}
          </Typography>
        </Stack>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            {valor}
          </Typography>
          {variacao != null && <ChipVariacao variacao={variacao} />}
        </Stack>
        {rodape && <Box sx={{ mt: 1 }}>{rodape}</Box>}
        {sparklineDados && <Sparkline dados={sparklineDados} cor={cor} />}
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

  const seriePedidos = vendas?.map((p) => p.pedidos) ?? [];
  const serieFaturamento = vendas?.map((p) => p.faturamento) ?? [];
  const variacaoPedidos = variacaoPercentual(seriePedidos);
  const variacaoFaturamento = variacaoPercentual(serieFaturamento);

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Dashboard
      </Typography>

      {data && <AcoesHoje resumo={data} />}

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
              cor="#3987e5"
              variacao={variacaoPedidos}
              sparklineDados={seriePedidos.slice(-8)}
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
              cor="#0ca30c"
              variacao={variacaoFaturamento}
              sparklineDados={serieFaturamento.slice(-8)}
            />
            <CartaoIndicador
              titulo="Impressões em andamento"
              valor={data.impressoesEmAndamento}
              icone={<PrintIcon />}
              cor="#199e70"
            />
            <CartaoIndicador
              titulo="Clientes ativos"
              valor={data.clientesAtivos}
              icone={<GroupIcon />}
              cor="#9085e9"
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
                {...TOOLTIP_ESTILO}
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
                {...TOOLTIP_ESTILO}
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
                {...TOOLTIP_ESTILO}
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

        <ResumoFinanceiroDonut />

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
                {...TOOLTIP_ESTILO}
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
                  {...TOOLTIP_ESTILO}
                  formatter={(valor) => [moeda.format(Number(valor)), 'Valor total']}
                />
                <Bar dataKey="valorTotal" fill={AZUL} radius={[0, 4, 4, 0]} maxBarSize={22} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </CartaoGrafico>

        {/* Taxa de sucesso reaproveitada como gauge — mesmo dado, forma radial. */}
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
                  gap: 1,
                }}
              >
                <Box sx={{ width: '100%', maxWidth: 220, height: 130, position: 'relative' }}>
                  <ResponsiveContainer>
                    <RadialBarChart
                      innerRadius="70%"
                      outerRadius="100%"
                      startAngle={180}
                      endAngle={0}
                      data={[{ nome: 'taxa', valor: taxa.taxaSucesso ?? 0 }]}
                    >
                      <PolarAngleAxis type="number" domain={[0, 100]} tick={false} />
                      <RadialBar
                        dataKey="valor"
                        cornerRadius={8}
                        fill="#0ca30c"
                        background={{ fill: 'rgba(231, 237, 243, 0.12)' }}
                        isAnimationActive={false}
                      />
                    </RadialBarChart>
                  </ResponsiveContainer>
                  <Box
                    sx={{
                      position: 'absolute',
                      inset: 0,
                      display: 'flex',
                      alignItems: 'flex-end',
                      justifyContent: 'center',
                      pb: 1,
                      pointerEvents: 'none',
                    }}
                  >
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                      {taxa.taxaSucesso != null ? `${String(taxa.taxaSucesso).replace('.', ',')}%` : '—'}
                    </Typography>
                  </Box>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
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

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          mt: 2,
          gridTemplateColumns: { xs: '1fr', lg: 'repeat(3, 1fr)' },
        }}
      >
        <ProducaoAndamento />
        <EstoqueCritico />
        <AtividadeRecente />
      </Box>
    </Box>
  );
}
