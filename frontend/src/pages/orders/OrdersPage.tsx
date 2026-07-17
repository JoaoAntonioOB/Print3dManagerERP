import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ReceiptIcon from '@mui/icons-material/Receipt';
import SyncAltIcon from '@mui/icons-material/SyncAlt';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  Alert,
  Box,
  Button,
  Chip,
  IconButton,
  Paper,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { mensagemDeErro } from '../../api/client';
import {
  CORES_STATUS_PEDIDO,
  ordersApi,
  ROTULOS_STATUS_PEDIDO,
  type OrderStatus,
  type PedidoResumo,
} from '../../api/orders';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { OrderFormDialog } from './OrderFormDialog';
import { OrderStatusDialog } from './OrderStatusDialog';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataBr = (iso: string | null) =>
  iso ? new Date(`${iso.slice(0, 10)}T00:00:00`).toLocaleDateString('pt-BR') : '—';
const dataHoraBr = (iso: string) => new Date(iso).toLocaleDateString('pt-BR');

export function OrdersPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);
  const podeFaturar =
    usuario !== null && ['ADMINISTRADOR', 'FINANCEIRO'].includes(usuario.role);

  const [busca, setBusca] = useState('');
  const [status, setStatus] = useState<OrderStatus | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [pedidoIdAberto, setPedidoIdAberto] = useState<number | null>(null);
  const [pedidoStatus, setPedidoStatus] = useState<PedidoResumo | null>(null);
  const [pedidoParaExcluir, setPedidoParaExcluir] = useState<PedidoResumo | null>(null);
  const [pedidoParaFaturar, setPedidoParaFaturar] = useState<PedidoResumo | null>(null);

  const filtros = { busca: buscaEstavel, status, page, size };

  const { data, isPending, error } = useQuery({
    queryKey: ['orders', filtros],
    queryFn: () => ordersApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const excluir = useMutation({
    mutationFn: (id: number) => ordersApi.excluir(id),
    onSuccess: () => {
      toast.success('Pedido excluído.');
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setPedidoParaExcluir(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const faturar = useMutation({
    mutationFn: (id: number) => ordersApi.faturar(id),
    onSuccess: (receita) => {
      toast.success(
        `Receita de ${moeda.format(receita.valor)} lançada no Financeiro (PENDENTE).`,
      );
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setPedidoParaFaturar(null);
    },
    onError: (erro) => {
      toast.error(mensagemDeErro(erro));
      setPedidoParaFaturar(null);
    },
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  const totalColunas = 7;

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 2,
          alignItems: 'center',
          justifyContent: 'space-between',
          mb: 3,
        }}
      >
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          Pedidos
        </Typography>
        {podeGerenciar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setPedidoIdAberto(null);
              setFormAberto(true);
            }}
          >
            Novo pedido
          </Button>
        )}
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Buscar"
          placeholder="Número ou nome do cliente"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 220 }}
        />
        <TextField
          label="Status"
          select
          size="small"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as OrderStatus | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 170 }}
        >
          <option value="">Todos</option>
          {(Object.keys(ROTULOS_STATUS_PEDIDO) as OrderStatus[]).map((valor) => (
            <option key={valor} value={valor}>
              {ROTULOS_STATUS_PEDIDO[valor]}
            </option>
          ))}
        </TextField>
      </Paper>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Número</TableCell>
              <TableCell>Cliente</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Aberto em</TableCell>
              <TableCell>Entrega prevista</TableCell>
              <TableCell align="right">Valor</TableCell>
              <TableCell align="right">Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isPending && !data ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={totalColunas}>
                    <Skeleton />
                  </TableCell>
                </TableRow>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((pedido) => (
                <TableRow key={pedido.id} hover>
                  <TableCell>{pedido.numero}</TableCell>
                  <TableCell>{pedido.clienteNome}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={CORES_STATUS_PEDIDO[pedido.status]}
                      label={ROTULOS_STATUS_PEDIDO[pedido.status]}
                    />
                  </TableCell>
                  <TableCell>{dataHoraBr(pedido.criadoEm)}</TableCell>
                  <TableCell>{dataBr(pedido.dataEntregaPrevista)}</TableCell>
                  <TableCell align="right">{moeda.format(pedido.valorTotal)}</TableCell>
                  <TableCell align="right">
                    <Tooltip title={pedido.status === 'PENDENTE' ? 'Editar' : 'Ver detalhes'}>
                      <IconButton
                        size="small"
                        aria-label="Detalhes"
                        onClick={() => {
                          setPedidoIdAberto(pedido.id);
                          setFormAberto(true);
                        }}
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {podeGerenciar && (
                      <Tooltip title="Mudar status">
                        <span>
                          <IconButton
                            size="small"
                            color="primary"
                            aria-label="Mudar status"
                            disabled={
                              pedido.status === 'ENTREGUE' || pedido.status === 'CANCELADO'
                            }
                            onClick={() => setPedidoStatus(pedido)}
                          >
                            <SyncAltIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                    {podeFaturar && (
                      <Tooltip title="Faturar (gera receita)">
                        <span>
                          <IconButton
                            size="small"
                            color="secondary"
                            aria-label="Faturar"
                            disabled={
                              pedido.status !== 'CONCLUIDO' && pedido.status !== 'ENTREGUE'
                            }
                            onClick={() => setPedidoParaFaturar(pedido)}
                          >
                            <ReceiptIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                    {podeGerenciar && (
                      <Tooltip title="Excluir (só pendente)">
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Excluir"
                            disabled={pedido.status !== 'PENDENTE'}
                            onClick={() => setPedidoParaExcluir(pedido)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={totalColunas} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Nenhum pedido encontrado.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={data?.totalElements ?? 0}
          page={page}
          onPageChange={(_, novaPagina) => setPage(novaPagina)}
          rowsPerPage={size}
          onRowsPerPageChange={(e) => {
            setSize(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[5, 10, 25, 50]}
        />
      </TableContainer>

      <OrderFormDialog
        aberto={formAberto}
        pedidoId={pedidoIdAberto}
        onFechar={() => setFormAberto(false)}
      />

      <OrderStatusDialog pedido={pedidoStatus} onFechar={() => setPedidoStatus(null)} />

      <ConfirmDialog
        aberto={pedidoParaExcluir !== null}
        titulo="Excluir pedido"
        mensagem={`O pedido ${pedidoParaExcluir?.numero} e seus itens serão excluídos definitivamente. Deseja continuar?`}
        rotuloConfirmar="Excluir"
        processando={excluir.isPending}
        onConfirmar={() => pedidoParaExcluir && excluir.mutate(pedidoParaExcluir.id)}
        onCancelar={() => setPedidoParaExcluir(null)}
      />

      <ConfirmDialog
        aberto={pedidoParaFaturar !== null}
        titulo="Faturar pedido"
        mensagem={`Será lançada uma receita PENDENTE de ${
          pedidoParaFaturar ? moeda.format(pedidoParaFaturar.valorTotal) : ''
        } vinculada ao pedido ${pedidoParaFaturar?.numero} no Financeiro. Confirmar?`}
        rotuloConfirmar="Faturar"
        corConfirmar="primary"
        processando={faturar.isPending}
        onConfirmar={() => pedidoParaFaturar && faturar.mutate(pedidoParaFaturar.id)}
        onCancelar={() => setPedidoParaFaturar(null)}
      />
    </Box>
  );
}
