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
import { inventoryApi, type ItemEstoque } from '../../api/inventory';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { InventoryFormDialog } from './InventoryFormDialog';
import { InventoryStockDialog } from './InventoryStockDialog';

const numero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function InventoryPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);

  const [busca, setBusca] = useState('');
  const [categoria, setCategoria] = useState('');
  const [situacao, setSituacao] = useState<'' | 'true' | 'false'>('');
  const [soEstoqueBaixo, setSoEstoqueBaixo] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);
  const categoriaEstavel = useDebounce(categoria);

  const [formAberto, setFormAberto] = useState(false);
  const [itemEmEdicao, setItemEmEdicao] = useState<ItemEstoque | null>(null);
  const [itemEstoque, setItemEstoque] = useState<ItemEstoque | null>(null);
  const [itemParaDesativar, setItemParaDesativar] = useState<ItemEstoque | null>(null);

  const filtros = {
    busca: buscaEstavel,
    categoria: categoriaEstavel,
    ativo: situacao === '' ? ('' as const) : situacao === 'true',
    estoqueBaixo: soEstoqueBaixo,
    page,
    size,
  };

  const { data, isPending, error } = useQuery({
    queryKey: ['inventory', filtros],
    queryFn: () => inventoryApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const desativar = useMutation({
    mutationFn: (id: number) => inventoryApi.desativar(id),
    onSuccess: () => {
      toast.success('Item desativado.');
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      setItemParaDesativar(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const reativar = useMutation({
    mutationFn: (id: number) => inventoryApi.reativar(id),
    onSuccess: () => {
      toast.success('Item reativado.');
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
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
          Estoque de insumos
        </Typography>
        {podeGerenciar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setItemEmEdicao(null);
              setFormAberto(true);
            }}
          >
            Novo item
          </Button>
        )}
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center' }}>
        <TextField
          label="Buscar"
          placeholder="Nome, descrição ou localização"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 200 }}
        />
        <TextField
          label="Categoria"
          size="small"
          value={categoria}
          onChange={(e) => {
            setCategoria(e.target.value);
            setPage(0);
          }}
          sx={{ minWidth: 160 }}
        />
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
              <TableCell>Categoria</TableCell>
              <TableCell align="right">Quantidade</TableCell>
              <TableCell>Unidade</TableCell>
              <TableCell align="right">Custo unitário</TableCell>
              <TableCell>Localização</TableCell>
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
              data.content.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>{item.nome}</TableCell>
                  <TableCell>{item.categoria ?? '—'}</TableCell>
                  <TableCell align="right">
                    <Box
                      component="span"
                      sx={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 0.5,
                        color: item.estoqueBaixo ? 'warning.main' : 'inherit',
                        fontWeight: item.estoqueBaixo ? 700 : 400,
                      }}
                    >
                      {item.estoqueBaixo && <WarningAmberIcon fontSize="inherit" />}
                      {numero.format(item.quantidade)}
                    </Box>
                  </TableCell>
                  <TableCell>{item.unidadeMedida}</TableCell>
                  <TableCell align="right">
                    {item.custoUnitario != null ? moeda.format(item.custoUnitario) : '—'}
                  </TableCell>
                  <TableCell>{item.localizacao ?? '—'}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={item.ativo ? 'success' : 'default'}
                      label={item.ativo ? 'Ativo' : 'Inativo'}
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
                            disabled={!item.ativo}
                            onClick={() => setItemEstoque(item)}
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
                            setItemEmEdicao(item);
                            setFormAberto(true);
                          }}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {item.ativo ? (
                        <Tooltip title="Desativar">
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Desativar"
                            onClick={() => setItemParaDesativar(item)}
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
                            onClick={() => reativar.mutate(item.id)}
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
                  <Typography color="text.secondary">Nenhum item encontrado.</Typography>
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

      <InventoryFormDialog
        aberto={formAberto}
        item={itemEmEdicao}
        onFechar={() => setFormAberto(false)}
      />

      <InventoryStockDialog item={itemEstoque} onFechar={() => setItemEstoque(null)} />

      <ConfirmDialog
        aberto={itemParaDesativar !== null}
        titulo="Desativar item"
        mensagem={`O item "${itemParaDesativar?.nome}" ficará indisponível para movimentações. Deseja continuar?`}
        rotuloConfirmar="Desativar"
        processando={desativar.isPending}
        onConfirmar={() => itemParaDesativar && desativar.mutate(itemParaDesativar.id)}
        onCancelar={() => setItemParaDesativar(null)}
      />
    </Box>
  );
}
