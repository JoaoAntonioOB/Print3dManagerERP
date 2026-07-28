import { Box } from '@mui/material';
import { Area, AreaChart, ResponsiveContainer } from 'recharts';

interface SparklineProps {
  dados: number[];
  cor: string;
}

/** Mini-gráfico sem eixos/grid/tooltip — só a tendência, dentro de um card de indicador. */
export function Sparkline({ dados, cor }: SparklineProps) {
  if (dados.length < 2) {
    return null;
  }
  const pontos = dados.map((valor, indice) => ({ indice, valor }));
  const id = `sparkline-${cor.replace('#', '')}`;

  return (
    <Box sx={{ height: 36, mt: 0.5 }}>
      <ResponsiveContainer>
        <AreaChart data={pontos} margin={{ top: 2, right: 0, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={cor} stopOpacity={0.35} />
              <stop offset="100%" stopColor={cor} stopOpacity={0} />
            </linearGradient>
          </defs>
          <Area
            type="monotone"
            dataKey="valor"
            stroke={cor}
            strokeWidth={2}
            fill={`url(#${id})`}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </Box>
  );
}
