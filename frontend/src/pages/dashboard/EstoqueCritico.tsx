import {
  Box,
  Button,
  Card,
  CardContent,
  LinearProgress,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { filamentsApi } from '../../api/filaments';
import { inventoryApi } from '../../api/inventory';

interface ItemCritico {
  chave: string;
  nome: string;
  restante: string;
  percentual: number;
  rota: string;
}

export function EstoqueCritico() {
  const navigate = useNavigate();

  const { data: filamentos, isPending: carregandoFilamentos } = useQuery({
    queryKey: ['filaments', 'estoque-baixo-painel'],
    queryFn: () => filamentsApi.listar({ estoqueBaixo: true, ativo: true, page: 0, size: 5 }),
  });

  const { data: insumos, isPending: carregandoInsumos } = useQuery({
    queryKey: ['inventory', 'estoque-baixo-painel'],
    queryFn: () => inventoryApi.listar({ estoqueBaixo: true, ativo: true, page: 0, size: 5 }),
  });

  const carregando = carregandoFilamentos || carregandoInsumos;

  const itens: ItemCritico[] = [
    ...(filamentos?.content ?? []).map((f) => ({
      chave: `filamento-${f.id}`,
      nome: `${f.nome} (filamento)`,
      restante: `${(f.quantidadeEstoqueG / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 2 })} kg restantes`,
      percentual: f.estoqueMinimoG > 0 ? (f.quantidadeEstoqueG / f.estoqueMinimoG) * 100 : 0,
      rota: '/filamentos',
    })),
    ...(insumos?.content ?? []).map((i) => ({
      chave: `insumo-${i.id}`,
      nome: i.nome,
      restante: `${i.quantidade.toLocaleString('pt-BR', { maximumFractionDigits: 2 })} ${i.unidadeMedida} restantes`,
      percentual: i.quantidadeMinima > 0 ? (i.quantidade / i.quantidadeMinima) * 100 : 0,
      rota: '/estoque',
    })),
  ].slice(0, 6);

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1.5 }}>
          Estoque crítico
        </Typography>
        {carregando ? (
          <Skeleton variant="rounded" height={200} />
        ) : itens.length === 0 ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 3, textAlign: 'center' }}>
            Nenhum item com estoque baixo.
          </Typography>
        ) : (
          <Stack spacing={1.5} sx={{ flexGrow: 1 }}>
            {itens.map((item) => (
              <Box key={item.chave}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.3 }}>
                  <Typography variant="body2" noWrap sx={{ maxWidth: '65%' }}>
                    {item.nome}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {item.restante}
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(item.percentual, 100)}
                  color={item.percentual < 50 ? 'error' : 'warning'}
                  sx={{ borderRadius: 1, height: 6 }}
                />
              </Box>
            ))}
          </Stack>
        )}
        <Button size="small" sx={{ mt: 2, alignSelf: 'flex-start' }} onClick={() => navigate('/filamentos')}>
          Ir para estoque
        </Button>
      </CardContent>
    </Card>
  );
}
