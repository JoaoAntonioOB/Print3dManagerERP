import CategoryIcon from '@mui/icons-material/Category';
import DownloadIcon from '@mui/icons-material/Download';
import PaidIcon from '@mui/icons-material/Paid';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import {
  Box,
  Button,
  Card,
  CardContent,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState, type ReactElement } from 'react';
import toast from 'react-hot-toast';
import { mensagemDeErro } from '../../api/client';
import { ROTULOS_STATUS_PEDIDO, type OrderStatus } from '../../api/orders';
import { baixarRelatorio, type TipoRelatorio } from '../../api/reports';
import { useAuth } from '../../auth/AuthContext';

/** Primeiro e último dia do mês corrente, em ISO (mesmo default do backend). */
function mesCorrente(): { de: string; ate: string } {
  const agora = new Date();
  const de = new Date(agora.getFullYear(), agora.getMonth(), 1);
  const ate = new Date(agora.getFullYear(), agora.getMonth() + 1, 0);
  const iso = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
      d.getDate(),
    ).padStart(2, '0')}`;
  return { de: iso(de), ate: iso(ate) };
}

function CartaoRelatorio({
  titulo,
  descricao,
  icone,
  baixando,
  onBaixar,
  extra,
}: {
  titulo: string;
  descricao: string;
  icone: ReactElement;
  baixando: boolean;
  onBaixar: () => void;
  extra?: ReactElement;
}) {
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
          <Box sx={{ color: 'primary.main', display: 'flex' }}>{icone}</Box>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {titulo}
          </Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {descricao}
        </Typography>
        {extra}
      </CardContent>
      <Box sx={{ p: 2, pt: 0 }}>
        <Button
          variant="contained"
          startIcon={<DownloadIcon />}
          disabled={baixando}
          onClick={onBaixar}
          fullWidth
        >
          {baixando ? 'Gerando…' : 'Baixar PDF'}
        </Button>
      </Box>
    </Card>
  );
}

export function ReportsPage() {
  const { usuario } = useAuth();
  const podeVerFinanceiro =
    usuario !== null && ['ADMINISTRADOR', 'FINANCEIRO'].includes(usuario.role);

  const [{ de, ate }, setPeriodo] = useState(mesCorrente());
  const [statusPedidos, setStatusPedidos] = useState<OrderStatus | ''>('');
  const [baixando, setBaixando] = useState<TipoRelatorio | null>(null);

  const baixar = async (tipo: TipoRelatorio) => {
    if (de && ate && de > ate) {
      toast.error('O início do período não pode ser depois do fim.');
      return;
    }
    setBaixando(tipo);
    try {
      const nome = await baixarRelatorio(tipo, {
        de,
        ate,
        status: tipo === 'pedidos' ? statusPedidos : undefined,
      });
      toast.success(`Relatório ${nome} baixado.`);
    } catch (erro) {
      toast.error(mensagemDeErro(erro));
    } finally {
      setBaixando(null);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Relatórios
      </Typography>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Início do período"
          type="date"
          size="small"
          value={de}
          onChange={(e) => setPeriodo((p) => ({ ...p, de: e.target.value }))}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <TextField
          label="Fim do período"
          type="date"
          size="small"
          value={ate}
          onChange={(e) => setPeriodo((p) => ({ ...p, ate: e.target.value }))}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
          O período vale para todos os relatórios (padrão: mês corrente).
        </Typography>
      </Paper>

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(3, 1fr)' },
        }}
      >
        <CartaoRelatorio
          titulo="Pedidos"
          descricao="Pedidos abertos no período, com cliente, status, entrega e valores. Totais de quantidade, cancelados e valor."
          icone={<ReceiptLongIcon />}
          baixando={baixando === 'pedidos'}
          onBaixar={() => baixar('pedidos')}
          extra={
            <TextField
              label="Status (opcional)"
              select
              size="small"
              fullWidth
              value={statusPedidos}
              onChange={(e) => setStatusPedidos(e.target.value as OrderStatus | '')}
              slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
            >
              <option value="">Todos</option>
              {(Object.keys(ROTULOS_STATUS_PEDIDO) as OrderStatus[]).map((valor) => (
                <option key={valor} value={valor}>
                  {ROTULOS_STATUS_PEDIDO[valor]}
                </option>
              ))}
            </TextField>
          }
        />
        {podeVerFinanceiro && (
          <CartaoRelatorio
            titulo="Financeiro"
            descricao="Transações do período com situação, canceladas identificadas e resumo de receitas, despesas e saldos."
            icone={<PaidIcon />}
            baixando={baixando === 'financeiro'}
            onBaixar={() => baixar('financeiro')}
          />
        )}
        <CartaoRelatorio
          titulo="Consumo de filamento"
          descricao="Impressões finalizadas com peso registrado, agregadas por filamento. Totais de jobs, gramas e custo."
          icone={<CategoryIcon />}
          baixando={baixando === 'consumo-filamento'}
          onBaixar={() => baixar('consumo-filamento')}
        />
      </Box>
    </Box>
  );
}
