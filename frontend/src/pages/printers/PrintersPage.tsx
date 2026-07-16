import AddIcon from '@mui/icons-material/Add';
import BlockIcon from '@mui/icons-material/Block';
import EditIcon from '@mui/icons-material/Edit';
import RestoreIcon from '@mui/icons-material/Restore';
import SettingsIcon from '@mui/icons-material/Settings';
import SyncAltIcon from '@mui/icons-material/SyncAlt';
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
import { printersApi, type Impressora, type PrinterStatus } from '../../api/printers';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { PrinterConfigDialog } from './PrinterConfigDialog';
import { PrinterFormDialog } from './PrinterFormDialog';
import { PrinterStatusDialog } from './PrinterStatusDialog';

const ROTULOS_STATUS: Record<PrinterStatus, string> = {
  DISPONIVEL: 'Disponível',
  IMPRIMINDO: 'Imprimindo',
  EM_MANUTENCAO: 'Em manutenção',
  INATIVA: 'Inativa',
};

const CORES_STATUS: Record<PrinterStatus, 'success' | 'info' | 'warning' | 'default'> = {
  DISPONIVEL: 'success',
  IMPRIMINDO: 'info',
  EM_MANUTENCAO: 'warning',
  INATIVA: 'default',
};

const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });

export function PrintersPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);
  const ehAdmin = usuario?.role === 'ADMINISTRADOR';

  const [busca, setBusca] = useState('');
  const [status, setStatus] = useState<PrinterStatus | ''>('');
  const [situacao, setSituacao] = useState<'' | 'true' | 'false'>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [impressoraEmEdicao, setImpressoraEmEdicao] = useState<Impressora | null>(null);
  const [impressoraStatus, setImpressoraStatus] = useState<Impressora | null>(null);
  const [configAberta, setConfigAberta] = useState(false);
  const [impressoraConfig, setImpressoraConfig] = useState<Impressora | null>(null);
  const [impressoraParaDesativar, setImpressoraParaDesativar] = useState<Impressora | null>(
    null,
  );

  const filtros = {
    busca: buscaEstavel,
    status,
    ativo: situacao === '' ? ('' as const) : situacao === 'true',
    page,
    size,
  };

  const { data, isPending, error } = useQuery({
    queryKey: ['printers', filtros],
    queryFn: () => printersApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const desativar = useMutation({
    mutationFn: (id: number) => printersApi.desativar(id),
    onSuccess: () => {
      toast.success('Impressora desativada.');
      queryClient.invalidateQueries({ queryKey: ['printers'] });
      setImpressoraParaDesativar(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const reativar = useMutation({
    mutationFn: (id: number) => printersApi.reativar(id),
    onSuccess: () => {
      toast.success('Impressora reativada.');
      queryClient.invalidateQueries({ queryKey: ['printers'] });
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  const totalColunas = podeGerenciar ? 8 : 7;

  const volume = (impressora: Impressora) =>
    impressora.volumeXMm && impressora.volumeYMm && impressora.volumeZMm
      ? `${impressora.volumeXMm} × ${impressora.volumeYMm} × ${impressora.volumeZMm}`
      : '—';

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
          Impressoras
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {ehAdmin && (
            <Button
              variant="outlined"
              startIcon={<SettingsIcon />}
              onClick={() => {
                setImpressoraConfig(null);
                setConfigAberta(true);
              }}
            >
              Config. global de custos
            </Button>
          )}
          {podeGerenciar && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                setImpressoraEmEdicao(null);
                setFormAberto(true);
              }}
            >
              Nova impressora
            </Button>
          )}
        </Box>
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Buscar"
          placeholder="Nome, marca ou modelo"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 200 }}
        />
        <TextField
          label="Status"
          select
          size="small"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as PrinterStatus | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 170 }}
        >
          <option value="">Todos</option>
          {(Object.keys(ROTULOS_STATUS) as PrinterStatus[]).map((valor) => (
            <option key={valor} value={valor}>
              {ROTULOS_STATUS[valor]}
            </option>
          ))}
        </TextField>
        <TextField
          label="Situação"
          select
          size="small"
          value={situacao}
          onChange={(e) => {
            setSituacao(e.target.value as '' | 'true' | 'false');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 130 }}
        >
          <option value="">Todas</option>
          <option value="true">Ativas</option>
          <option value="false">Inativas</option>
        </TextField>
      </Paper>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nome</TableCell>
              <TableCell>Marca / Modelo</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Potência (W)</TableCell>
              <TableCell>Volume (mm)</TableCell>
              <TableCell align="right">Horas totais</TableCell>
              <TableCell>Situação</TableCell>
              {podeGerenciar && <TableCell align="right">Ações</TableCell>}
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
              data.content.map((impressora) => (
                <TableRow key={impressora.id} hover>
                  <TableCell>{impressora.nome}</TableCell>
                  <TableCell>
                    {[impressora.marca, impressora.modelo].filter(Boolean).join(' / ') || '—'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={CORES_STATUS[impressora.status]}
                      label={ROTULOS_STATUS[impressora.status]}
                    />
                  </TableCell>
                  <TableCell align="right">{impressora.potenciaWatts ?? '—'}</TableCell>
                  <TableCell>{volume(impressora)}</TableCell>
                  <TableCell align="right">
                    {numero.format(impressora.horasImpressaoTotal)}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={impressora.ativo ? 'success' : 'default'}
                      label={impressora.ativo ? 'Ativa' : 'Inativa'}
                    />
                  </TableCell>
                  {podeGerenciar && (
                    <TableCell align="right">
                      <Tooltip title="Mudar status">
                        <span>
                          <IconButton
                            size="small"
                            color="primary"
                            aria-label="Mudar status"
                            disabled={!impressora.ativo}
                            onClick={() => setImpressoraStatus(impressora)}
                          >
                            <SyncAltIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="Custos">
                        <IconButton
                          size="small"
                          aria-label="Custos"
                          onClick={() => {
                            setImpressoraConfig(impressora);
                            setConfigAberta(true);
                          }}
                        >
                          <SettingsIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Editar">
                        <IconButton
                          size="small"
                          aria-label="Editar"
                          onClick={() => {
                            setImpressoraEmEdicao(impressora);
                            setFormAberto(true);
                          }}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {impressora.ativo ? (
                        <Tooltip title="Desativar">
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Desativar"
                            onClick={() => setImpressoraParaDesativar(impressora)}
                          >
                            <BlockIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      ) : (
                        <Tooltip title="Reativar">
                          <IconButton
                            size="small"
                            color="success"
                            aria-label="Reativar"
                            onClick={() => reativar.mutate(impressora.id)}
                            disabled={reativar.isPending}
                          >
                            <RestoreIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={totalColunas} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Nenhuma impressora encontrada.</Typography>
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

      <PrinterFormDialog
        aberto={formAberto}
        impressora={impressoraEmEdicao}
        onFechar={() => setFormAberto(false)}
      />

      <PrinterStatusDialog
        impressora={impressoraStatus}
        onFechar={() => setImpressoraStatus(null)}
      />

      <PrinterConfigDialog
        aberto={configAberta}
        impressora={impressoraConfig}
        onFechar={() => setConfigAberta(false)}
      />

      <ConfirmDialog
        aberto={impressoraParaDesativar !== null}
        titulo="Desativar impressora"
        mensagem={`A impressora "${impressoraParaDesativar?.nome}" ficará INATIVA e indisponível para novas impressões. Deseja continuar?`}
        rotuloConfirmar="Desativar"
        processando={desativar.isPending}
        onConfirmar={() =>
          impressoraParaDesativar && desativar.mutate(impressoraParaDesativar.id)
        }
        onCancelar={() => setImpressoraParaDesativar(null)}
      />
    </Box>
  );
}
