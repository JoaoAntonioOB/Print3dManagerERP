import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
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
import {
  CORES_STATUS_TRANSACAO,
  financialApi,
  ROTULOS_STATUS_TRANSACAO,
  ROTULOS_TIPO_TRANSACAO,
  TRANSICOES_TRANSACAO,
  type Transacao,
  type TransactionStatus,
  type TransactionType,
} from '../../api/financial';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { TransactionFormDialog } from './TransactionFormDialog';
import { TransactionStatusDialog } from './TransactionStatusDialog';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataBr = (iso: string) =>
  new Date(`${iso.slice(0, 10)}T00:00:00`).toLocaleDateString('pt-BR');

/** Card de indicador do resumo do mês corrente. */
function CardResumo({ titulo, valor, cor }: {
  titulo: string;
  valor: number | undefined;
  cor?: string;
}) {
  return (
    <Paper sx={{ p: 2, flex: '1 1 150px', minWidth: 150 }}>
      <Typography variant="body2" color="text.secondary">
        {titulo}
      </Typography>
      {valor === undefined ? (
        <Skeleton width={90} height={32} />
      ) : (
        <Typography variant="h6" sx={{ fontWeight: 700, color: cor }}>
          {moeda.format(valor)}
        </Typography>
      )}
    </Paper>
  );
}

export function FinancialPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  // Financeiro foge do padrão dos CRUDs: quem gerencia é ADMINISTRADOR/FINANCEIRO.
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'FINANCEIRO'].includes(usuario.role);

  const [busca, setBusca] = useState('');
  const [tipo, setTipo] = useState<TransactionType | ''>('');
  const [status, setStatus] = useState<TransactionStatus | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [transacaoEmEdicao, setTransacaoEmEdicao] = useState<Transacao | null>(null);
  const [transacaoStatus, setTransacaoStatus] = useState<Transacao | null>(null);
  const [transacaoParaExcluir, setTransacaoParaExcluir] = useState<Transacao | null>(null);

  const filtros = { busca: buscaEstavel, tipo, status, page, size };

  const { data, isPending, error } = useQuery({
    queryKey: ['financial', 'transactions', filtros],
    queryFn: () => financialApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const { data: resumo } = useQuery({
    queryKey: ['financial', 'resumo'],
    queryFn: () => financialApi.resumo(),
  });

  const excluir = useMutation({
    mutationFn: (id: number) => financialApi.excluir(id),
    onSuccess: () => {
      toast.success('Transação excluída.');
      queryClient.invalidateQueries({ queryKey: ['financial'] });
      setTransacaoParaExcluir(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  const totalColunas = 8;

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
          Financeiro
        </Typography>
        {podeGerenciar && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setTransacaoEmEdicao(null);
              setFormAberto(true);
            }}
          >
            Nova transação
          </Button>
        )}
      </Box>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
        <CardResumo titulo="Receitas pagas (mês)" valor={resumo?.receitasPagas} cor="success.main" />
        <CardResumo titulo="Receitas pendentes" valor={resumo?.receitasPendentes} />
        <CardResumo titulo="Despesas pagas (mês)" valor={resumo?.despesasPagas} cor="error.main" />
        <CardResumo titulo="Despesas pendentes" valor={resumo?.despesasPendentes} />
        <CardResumo titulo="Saldo realizado" valor={resumo?.saldoRealizado} cor="primary.main" />
        <CardResumo titulo="Saldo previsto" valor={resumo?.saldoPrevisto} />
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Buscar"
          placeholder="Descrição, categoria ou observações"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 220 }}
        />
        <TextField
          label="Tipo"
          select
          size="small"
          value={tipo}
          onChange={(e) => {
            setTipo(e.target.value as TransactionType | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 140 }}
        >
          <option value="">Todos</option>
          <option value="RECEITA">Receita</option>
          <option value="DESPESA">Despesa</option>
        </TextField>
        <TextField
          label="Situação"
          select
          size="small"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as TransactionStatus | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 150 }}
        >
          <option value="">Todas</option>
          {(Object.keys(ROTULOS_STATUS_TRANSACAO) as TransactionStatus[]).map((valor) => (
            <option key={valor} value={valor}>
              {ROTULOS_STATUS_TRANSACAO[valor]}
            </option>
          ))}
        </TextField>
      </Paper>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Data</TableCell>
              <TableCell>Descrição</TableCell>
              <TableCell>Categoria</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Vínculo</TableCell>
              <TableCell>Situação</TableCell>
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
              data.content.map((transacao) => (
                <TableRow key={transacao.id} hover>
                  <TableCell>{dataBr(transacao.dataTransacao)}</TableCell>
                  <TableCell>{transacao.descricao}</TableCell>
                  <TableCell>{transacao.categoria}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      variant="outlined"
                      color={transacao.tipo === 'RECEITA' ? 'success' : 'error'}
                      label={ROTULOS_TIPO_TRANSACAO[transacao.tipo]}
                    />
                  </TableCell>
                  <TableCell>
                    {transacao.pedidoNumero ?? transacao.clienteNome ?? '—'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={CORES_STATUS_TRANSACAO[transacao.status]}
                      label={ROTULOS_STATUS_TRANSACAO[transacao.status]}
                    />
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{
                      color: transacao.tipo === 'RECEITA' ? 'success.main' : 'error.main',
                      fontWeight: 600,
                    }}
                  >
                    {transacao.tipo === 'RECEITA' ? '+' : '−'}
                    {moeda.format(transacao.valor)}
                  </TableCell>
                  <TableCell align="right">
                    {podeGerenciar && (
                      <>
                        <Tooltip title="Editar (só pendente)">
                          <span>
                            <IconButton
                              size="small"
                              aria-label="Editar"
                              disabled={transacao.status !== 'PENDENTE'}
                              onClick={() => {
                                setTransacaoEmEdicao(transacao);
                                setFormAberto(true);
                              }}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip title="Mudar situação">
                          <span>
                            <IconButton
                              size="small"
                              color="primary"
                              aria-label="Mudar situação"
                              disabled={TRANSICOES_TRANSACAO[transacao.status].length === 0}
                              onClick={() => setTransacaoStatus(transacao)}
                            >
                              <SyncAltIcon fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip title="Excluir (só pendente)">
                          <span>
                            <IconButton
                              size="small"
                              color="error"
                              aria-label="Excluir"
                              disabled={transacao.status !== 'PENDENTE'}
                              onClick={() => setTransacaoParaExcluir(transacao)}
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
                    Nenhuma transação encontrada.
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

      <TransactionFormDialog
        aberto={formAberto}
        transacao={transacaoEmEdicao}
        onFechar={() => setFormAberto(false)}
      />

      <TransactionStatusDialog
        transacao={transacaoStatus}
        onFechar={() => setTransacaoStatus(null)}
      />

      <ConfirmDialog
        aberto={transacaoParaExcluir !== null}
        titulo="Excluir transação"
        mensagem={`A transação "${transacaoParaExcluir?.descricao}" será excluída definitivamente. Pagas ou canceladas ficam como histórico. Deseja continuar?`}
        rotuloConfirmar="Excluir"
        processando={excluir.isPending}
        onConfirmar={() => transacaoParaExcluir && excluir.mutate(transacaoParaExcluir.id)}
        onCancelar={() => setTransacaoParaExcluir(null)}
      />
    </Box>
  );
}
