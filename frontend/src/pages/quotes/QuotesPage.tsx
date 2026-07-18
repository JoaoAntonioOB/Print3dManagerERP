import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import LinkIcon from '@mui/icons-material/Link';
import ShoppingCartCheckoutIcon from '@mui/icons-material/ShoppingCartCheckout';
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
  CORES_STATUS_ORCAMENTO,
  quotesApi,
  ROTULOS_STATUS_ORCAMENTO,
  TRANSICOES_ORCAMENTO,
  type Orcamento,
  type QuoteStatus,
} from '../../api/quotes';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { QuoteFormDialog } from './QuoteFormDialog';
import { QuoteStatusDialog } from './QuoteStatusDialog';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataBr = (iso: string | null) =>
  iso ? new Date(`${iso.slice(0, 10)}T00:00:00`).toLocaleDateString('pt-BR') : '—';
const dataHoraBr = (iso: string) => new Date(iso).toLocaleDateString('pt-BR');

export function QuotesPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);

  const [busca, setBusca] = useState('');
  const [status, setStatus] = useState<QuoteStatus | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [orcamentoIdAberto, setOrcamentoIdAberto] = useState<number | null>(null);
  const [orcamentoStatus, setOrcamentoStatus] = useState<Orcamento | null>(null);
  const [orcamentoParaExcluir, setOrcamentoParaExcluir] = useState<Orcamento | null>(null);
  const [orcamentoParaConverter, setOrcamentoParaConverter] = useState<Orcamento | null>(null);

  const filtros = { busca: buscaEstavel, status, page, size };

  const { data, isPending, error } = useQuery({
    queryKey: ['quotes', filtros],
    queryFn: () => quotesApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const excluir = useMutation({
    mutationFn: (id: number) => quotesApi.excluir(id),
    onSuccess: () => {
      toast.success('Orçamento excluído.');
      queryClient.invalidateQueries({ queryKey: ['quotes'] });
      setOrcamentoParaExcluir(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const converter = useMutation({
    mutationFn: (id: number) => quotesApi.converter(id),
    onSuccess: (pedido) => {
      toast.success(`Orçamento convertido no pedido ${pedido.numero}.`);
      queryClient.invalidateQueries({ queryKey: ['quotes'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      setOrcamentoParaConverter(null);
    },
    onError: (erro) => {
      toast.error(mensagemDeErro(erro));
      setOrcamentoParaConverter(null);
    },
  });

  const copiarLink = async (orcamento: Orcamento) => {
    const link = `${window.location.origin}/orcamento/${orcamento.shareToken}`;
    try {
      await navigator.clipboard.writeText(link);
      toast.success('Link público copiado.');
    } catch {
      toast.error(`Não foi possível copiar. Link: ${link}`);
    }
  };

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
          Orçamentos
        </Typography>
        {podeGerenciar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setOrcamentoIdAberto(null);
              setFormAberto(true);
            }}
          >
            Novo orçamento
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
            setStatus(e.target.value as QuoteStatus | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 170 }}
        >
          <option value="">Todos</option>
          {(Object.keys(ROTULOS_STATUS_ORCAMENTO) as QuoteStatus[]).map((valor) => (
            <option key={valor} value={valor}>
              {ROTULOS_STATUS_ORCAMENTO[valor]}
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
              <TableCell>Criado em</TableCell>
              <TableCell>Validade</TableCell>
              <TableCell align="right">Preço ao cliente</TableCell>
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
              data.content.map((orcamento) => (
                <TableRow key={orcamento.id} hover>
                  <TableCell>{orcamento.numero}</TableCell>
                  <TableCell>{orcamento.clienteNome}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={CORES_STATUS_ORCAMENTO[orcamento.status]}
                      label={ROTULOS_STATUS_ORCAMENTO[orcamento.status]}
                    />
                  </TableCell>
                  <TableCell>{dataHoraBr(orcamento.criadoEm)}</TableCell>
                  <TableCell>{dataBr(orcamento.dataValidade)}</TableCell>
                  <TableCell align="right">{moeda.format(orcamento.precoEfetivo)}</TableCell>
                  <TableCell align="right">
                    <Tooltip
                      title={orcamento.status === 'RASCUNHO' ? 'Editar' : 'Ver detalhes'}
                    >
                      <IconButton
                        size="small"
                        aria-label="Detalhes"
                        onClick={() => {
                          setOrcamentoIdAberto(orcamento.id);
                          setFormAberto(true);
                        }}
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Copiar link público (aprovação do cliente)">
                      <span>
                        <IconButton
                          size="small"
                          aria-label="Copiar link"
                          disabled={orcamento.status === 'RASCUNHO'}
                          onClick={() => copiarLink(orcamento)}
                        >
                          <LinkIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    {podeGerenciar && (
                      <>
                        <Tooltip title="Mudar status">
                          <span>
                            <IconButton
                              size="small"
                              color="primary"
                              aria-label="Mudar status"
                              disabled={TRANSICOES_ORCAMENTO[orcamento.status].length === 0}
                              onClick={() => setOrcamentoStatus(orcamento)}
                            >
                              <SyncAltIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip title="Converter em pedido (só aprovado)">
                          <span>
                            <IconButton
                              size="small"
                              color="secondary"
                              aria-label="Converter"
                              disabled={orcamento.status !== 'APROVADO'}
                              onClick={() => setOrcamentoParaConverter(orcamento)}
                            >
                              <ShoppingCartCheckoutIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip title="Excluir (só rascunho)">
                          <span>
                            <IconButton
                              size="small"
                              color="error"
                              aria-label="Excluir"
                              disabled={orcamento.status !== 'RASCUNHO'}
                              onClick={() => setOrcamentoParaExcluir(orcamento)}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </span>
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
                    Nenhum orçamento encontrado.
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

      <QuoteFormDialog
        aberto={formAberto}
        orcamentoId={orcamentoIdAberto}
        onFechar={() => setFormAberto(false)}
      />

      <QuoteStatusDialog
        orcamento={orcamentoStatus}
        onFechar={() => setOrcamentoStatus(null)}
      />

      <ConfirmDialog
        aberto={orcamentoParaExcluir !== null}
        titulo="Excluir orçamento"
        mensagem={`O orçamento ${orcamentoParaExcluir?.numero} será excluído definitivamente. Deseja continuar?`}
        rotuloConfirmar="Excluir"
        processando={excluir.isPending}
        onConfirmar={() => orcamentoParaExcluir && excluir.mutate(orcamentoParaExcluir.id)}
        onCancelar={() => setOrcamentoParaExcluir(null)}
      />

      <ConfirmDialog
        aberto={orcamentoParaConverter !== null}
        titulo="Converter em pedido"
        mensagem={`Será criado um pedido com um item espelhando o orçamento ${
          orcamentoParaConverter?.numero
        } (preço ${
          orcamentoParaConverter ? moeda.format(orcamentoParaConverter.precoEfetivo) : ''
        }) e o orçamento ficará CONVERTIDO. Confirmar?`}
        rotuloConfirmar="Converter"
        corConfirmar="primary"
        processando={converter.isPending}
        onConfirmar={() =>
          orcamentoParaConverter && converter.mutate(orcamentoParaConverter.id)
        }
        onCancelar={() => setOrcamentoParaConverter(null)}
      />
    </Box>
  );
}
