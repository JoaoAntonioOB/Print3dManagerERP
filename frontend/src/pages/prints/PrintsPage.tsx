import AddIcon from '@mui/icons-material/Add';
import CancelIcon from '@mui/icons-material/Cancel';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
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
import { printersApi } from '../../api/printers';
import {
  CORES_STATUS_IMPRESSAO,
  printsApi,
  ROTULOS_STATUS_IMPRESSAO,
  type Impressao,
  type PrintStatus,
} from '../../api/prints';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { PrintFinishDialog, type ModoFinalizacao } from './PrintFinishDialog';
import { PrintStartDialog } from './PrintStartDialog';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHoraBr = (iso: string) =>
  new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function PrintsPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);

  const [impressoraId, setImpressoraId] = useState<number | ''>('');
  const [status, setStatus] = useState<PrintStatus | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [inicioAberto, setInicioAberto] = useState(false);
  const [finalizacao, setFinalizacao] = useState<{
    impressao: Impressao;
    modo: ModoFinalizacao;
  } | null>(null);
  const [impressaoParaCancelar, setImpressaoParaCancelar] = useState<Impressao | null>(null);

  const filtros = { impressoraId, status, page, size };

  const { data, isPending, error } = useQuery({
    queryKey: ['prints', filtros],
    queryFn: () => printsApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const { data: impressoras } = useQuery({
    queryKey: ['printers', 'select', 'todas'],
    queryFn: () => printersApi.listar({ page: 0, size: 100 }),
  });

  const cancelar = useMutation({
    mutationFn: (id: number) => printsApi.cancelar(id),
    onSuccess: () => {
      toast.success('Impressão cancelada. A impressora foi liberada.');
      queryClient.invalidateQueries({ queryKey: ['prints'] });
      queryClient.invalidateQueries({ queryKey: ['printers'] });
      setImpressaoParaCancelar(null);
    },
    onError: (erro) => {
      toast.error(mensagemDeErro(erro));
      setImpressaoParaCancelar(null);
    },
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  const totalColunas = 9;

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
          Impressões
        </Typography>
        {podeGerenciar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setInicioAberto(true)}
          >
            Nova impressão
          </Button>
        )}
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Impressora"
          select
          size="small"
          value={impressoraId === '' ? '' : String(impressoraId)}
          onChange={(e) => {
            setImpressoraId(e.target.value === '' ? '' : Number(e.target.value));
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 220 }}
        >
          <option value="">Todas</option>
          {impressoras?.content.map((impressora) => (
            <option key={impressora.id} value={String(impressora.id)}>
              {impressora.nome}
            </option>
          ))}
        </TextField>
        <TextField
          label="Status"
          select
          size="small"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as PrintStatus | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 170 }}
        >
          <option value="">Todos</option>
          {(Object.keys(ROTULOS_STATUS_IMPRESSAO) as PrintStatus[]).map((valor) => (
            <option key={valor} value={valor}>
              {ROTULOS_STATUS_IMPRESSAO[valor]}
            </option>
          ))}
        </TextField>
      </Paper>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Impressora</TableCell>
              <TableCell>Peça / Pedido</TableCell>
              <TableCell>Filamento</TableCell>
              <TableCell>Operador</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Iniciado em</TableCell>
              <TableCell align="right">Tempo (min)</TableCell>
              <TableCell align="right">Custo</TableCell>
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
              data.content.map((impressao) => (
                <TableRow key={impressao.id} hover>
                  <TableCell>{impressao.impressoraNome}</TableCell>
                  <TableCell>
                    {impressao.nomePeca ?? 'Avulsa'}
                    {impressao.pedidoNumero ? ` (${impressao.pedidoNumero})` : ''}
                  </TableCell>
                  <TableCell>{impressao.filamentoNome ?? '—'}</TableCell>
                  <TableCell>{impressao.usuarioNome}</TableCell>
                  <TableCell>
                    <Tooltip title={impressao.motivoFalha ?? ''}>
                      <Chip
                        size="small"
                        color={CORES_STATUS_IMPRESSAO[impressao.status]}
                        label={ROTULOS_STATUS_IMPRESSAO[impressao.status]}
                      />
                    </Tooltip>
                  </TableCell>
                  <TableCell>{dataHoraBr(impressao.iniciadoEm)}</TableCell>
                  <TableCell align="right">{impressao.tempoTotalMinutos ?? '—'}</TableCell>
                  <TableCell align="right">
                    {impressao.custoTotal != null ? moeda.format(impressao.custoTotal) : '—'}
                  </TableCell>
                  <TableCell align="right">
                    {podeGerenciar && impressao.status === 'EM_ANDAMENTO' && (
                      <>
                        <Tooltip title="Concluir">
                          <IconButton
                            size="small"
                            color="success"
                            aria-label="Concluir"
                            onClick={() => setFinalizacao({ impressao, modo: 'concluir' })}
                          >
                            <CheckCircleIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Registrar falha">
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Falhar"
                            onClick={() => setFinalizacao({ impressao, modo: 'falhar' })}
                          >
                            <ReportProblemIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Cancelar job">
                          <IconButton
                            size="small"
                            aria-label="Cancelar job"
                            onClick={() => setImpressaoParaCancelar(impressao)}
                          >
                            <CancelIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={totalColunas} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    Nenhuma impressão encontrada.
                  </Typography>
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

      <PrintStartDialog aberto={inicioAberto} onFechar={() => setInicioAberto(false)} />

      <PrintFinishDialog
        impressao={finalizacao?.impressao ?? null}
        modo={finalizacao?.modo ?? 'concluir'}
        onFechar={() => setFinalizacao(null)}
      />

      <ConfirmDialog
        aberto={impressaoParaCancelar !== null}
        titulo="Cancelar impressão"
        mensagem={`O job na impressora ${impressaoParaCancelar?.impressoraNome} será cancelado e a máquina liberada, sem consumo de estoque. Deseja continuar?`}
        rotuloConfirmar="Cancelar job"
        processando={cancelar.isPending}
        onConfirmar={() =>
          impressaoParaCancelar && cancelar.mutate(impressaoParaCancelar.id)
        }
        onCancelar={() => setImpressaoParaCancelar(null)}
      />
    </Box>
  );
}
