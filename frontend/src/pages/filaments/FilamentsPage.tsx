import AddIcon from '@mui/icons-material/Add';
import BlockIcon from '@mui/icons-material/Block';
import EditIcon from '@mui/icons-material/Edit';
import RestoreIcon from '@mui/icons-material/Restore';
import SwapVertIcon from '@mui/icons-material/SwapVert';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  FormControlLabel,
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
  filamentsApi,
  type Filamento,
  type FilamentMaterial,
} from '../../api/filaments';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { FilamentFormDialog } from './FilamentFormDialog';
import { FilamentStockDialog } from './FilamentStockDialog';

const MATERIAIS: FilamentMaterial[] = [
  'PLA',
  'ABS',
  'PETG',
  'TPU',
  'ASA',
  'NYLON',
  'RESINA',
  'OUTRO',
];

const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function FilamentsPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);

  const [busca, setBusca] = useState('');
  const [material, setMaterial] = useState<FilamentMaterial | ''>('');
  const [situacao, setSituacao] = useState<'' | 'true' | 'false'>('');
  const [soEstoqueBaixo, setSoEstoqueBaixo] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [filamentoEmEdicao, setFilamentoEmEdicao] = useState<Filamento | null>(null);
  const [filamentoEstoque, setFilamentoEstoque] = useState<Filamento | null>(null);
  const [filamentoParaDesativar, setFilamentoParaDesativar] = useState<Filamento | null>(null);

  const filtros = {
    busca: buscaEstavel,
    material,
    ativo: situacao === '' ? ('' as const) : situacao === 'true',
    estoqueBaixo: soEstoqueBaixo,
    page,
    size,
  };

  const { data, isPending, error } = useQuery({
    queryKey: ['filaments', filtros],
    queryFn: () => filamentsApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const desativar = useMutation({
    mutationFn: (id: number) => filamentsApi.desativar(id),
    onSuccess: () => {
      toast.success('Filamento desativado.');
      queryClient.invalidateQueries({ queryKey: ['filaments'] });
      setFilamentoParaDesativar(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const reativar = useMutation({
    mutationFn: (id: number) => filamentsApi.reativar(id),
    onSuccess: () => {
      toast.success('Filamento reativado.');
      queryClient.invalidateQueries({ queryKey: ['filaments'] });
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  const totalColunas = podeGerenciar ? 8 : 7;

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
          Filamentos
        </Typography>
        {podeGerenciar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setFilamentoEmEdicao(null);
              setFormAberto(true);
            }}
          >
            Novo filamento
          </Button>
        )}
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center' }}>
        <TextField
          label="Buscar"
          placeholder="Nome, marca ou cor"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 200 }}
        />
        <TextField
          label="Material"
          select
          size="small"
          value={material}
          onChange={(e) => {
            setMaterial(e.target.value as FilamentMaterial | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 130 }}
        >
          <option value="">Todos</option>
          {MATERIAIS.map((m) => (
            <option key={m} value={m}>
              {m}
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
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </TextField>
        <FormControlLabel
          control={
            <Checkbox
              checked={soEstoqueBaixo}
              onChange={(e) => {
                setSoEstoqueBaixo(e.target.checked);
                setPage(0);
              }}
            />
          }
          label="Só estoque baixo"
        />
      </Paper>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nome</TableCell>
              <TableCell>Marca</TableCell>
              <TableCell>Material</TableCell>
              <TableCell>Cor</TableCell>
              <TableCell align="right">Estoque (g)</TableCell>
              <TableCell align="right">Custo/kg</TableCell>
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
              data.content.map((filamento) => (
                <TableRow key={filamento.id} hover>
                  <TableCell>{filamento.nome}</TableCell>
                  <TableCell>{filamento.marca ?? '—'}</TableCell>
                  <TableCell>
                    <Chip size="small" variant="outlined" label={filamento.material} />
                  </TableCell>
                  <TableCell>{filamento.cor ?? '—'}</TableCell>
                  <TableCell align="right">
                    <Box
                      component="span"
                      sx={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 0.5,
                        color: filamento.estoqueBaixo ? 'warning.main' : 'inherit',
                        fontWeight: filamento.estoqueBaixo ? 700 : 400,
                      }}
                    >
                      {filamento.estoqueBaixo && <WarningAmberIcon fontSize="inherit" />}
                      {numero.format(filamento.quantidadeEstoqueG)}
                    </Box>
                  </TableCell>
                  <TableCell align="right">
                    {filamento.custoPorKg !== null ? moeda.format(filamento.custoPorKg) : '—'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={filamento.ativo ? 'success' : 'default'}
                      label={filamento.ativo ? 'Ativo' : 'Inativo'}
                    />
                  </TableCell>
                  {podeGerenciar && (
                    <TableCell align="right">
                      <Tooltip title="Movimentar estoque">
                        <span>
                          <IconButton
                            size="small"
                            color="primary"
                            aria-label="Movimentar estoque"
                            disabled={!filamento.ativo}
                            onClick={() => setFilamentoEstoque(filamento)}
                          >
                            <SwapVertIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="Editar">
                        <IconButton
                          size="small"
                          aria-label="Editar"
                          onClick={() => {
                            setFilamentoEmEdicao(filamento);
                            setFormAberto(true);
                          }}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {filamento.ativo ? (
                        <Tooltip title="Desativar">
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Desativar"
                            onClick={() => setFilamentoParaDesativar(filamento)}
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
                            onClick={() => reativar.mutate(filamento.id)}
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
                  <Typography color="text.secondary">Nenhum filamento encontrado.</Typography>
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

      <FilamentFormDialog
        aberto={formAberto}
        filamento={filamentoEmEdicao}
        onFechar={() => setFormAberto(false)}
      />

      <FilamentStockDialog
        filamento={filamentoEstoque}
        onFechar={() => setFilamentoEstoque(null)}
      />

      <ConfirmDialog
        aberto={filamentoParaDesativar !== null}
        titulo="Desativar filamento"
        mensagem={`O filamento "${filamentoParaDesativar?.nome}" ficará indisponível para novas impressões e movimentações. Deseja continuar?`}
        rotuloConfirmar="Desativar"
        processando={desativar.isPending}
        onConfirmar={() =>
          filamentoParaDesativar && desativar.mutate(filamentoParaDesativar.id)
        }
        onCancelar={() => setFilamentoParaDesativar(null)}
      />
    </Box>
  );
}
