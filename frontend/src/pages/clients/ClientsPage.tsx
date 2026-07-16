import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import RestoreIcon from '@mui/icons-material/Restore';
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
import { clientsApi, type Cliente, type PersonType } from '../../api/clients';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { ClientFormDialog } from './ClientFormDialog';

export function ClientsPage() {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeGerenciar =
    usuario !== null && ['ADMINISTRADOR', 'OPERADOR'].includes(usuario.role);

  const [busca, setBusca] = useState('');
  const [tipoPessoa, setTipoPessoa] = useState<PersonType | ''>('');
  const [situacao, setSituacao] = useState<'' | 'true' | 'false'>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [clienteEmEdicao, setClienteEmEdicao] = useState<Cliente | null>(null);
  const [clienteParaDesativar, setClienteParaDesativar] = useState<Cliente | null>(null);

  const filtros = {
    busca: buscaEstavel,
    tipoPessoa,
    ativo: situacao === '' ? ('' as const) : situacao === 'true',
    page,
    size,
  };

  const { data, isPending, error } = useQuery({
    queryKey: ['clients', filtros],
    queryFn: () => clientsApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const desativar = useMutation({
    mutationFn: (id: number) => clientsApi.desativar(id),
    onSuccess: () => {
      toast.success('Cliente desativado.');
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      setClienteParaDesativar(null);
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const reativar = useMutation({
    mutationFn: (id: number) => clientsApi.reativar(id),
    onSuccess: () => {
      toast.success('Cliente reativado.');
      queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const abrirCadastro = () => {
    setClienteEmEdicao(null);
    setFormAberto(true);
  };

  const abrirEdicao = (cliente: Cliente) => {
    setClienteEmEdicao(cliente);
    setFormAberto(true);
  };

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

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
          Clientes
        </Typography>
        {podeGerenciar && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={abrirCadastro}>
            Novo cliente
          </Button>
        )}
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Buscar"
          placeholder="Nome, e-mail ou CPF/CNPJ"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 220 }}
        />
        <TextField
          label="Tipo de pessoa"
          select
          size="small"
          value={tipoPessoa}
          onChange={(e) => {
            setTipoPessoa(e.target.value as PersonType | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 160 }}
        >
          <option value="">Todos</option>
          <option value="FISICA">Física</option>
          <option value="JURIDICA">Jurídica</option>
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
          sx={{ minWidth: 140 }}
        >
          <option value="">Todas</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </TextField>
      </Paper>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nome</TableCell>
              <TableCell>E-mail</TableCell>
              <TableCell>Telefone</TableCell>
              <TableCell>CPF/CNPJ</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Situação</TableCell>
              {podeGerenciar && <TableCell align="right">Ações</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {isPending && !data ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={podeGerenciar ? 7 : 6}>
                    <Skeleton />
                  </TableCell>
                </TableRow>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((cliente) => (
                <TableRow key={cliente.id} hover>
                  <TableCell>{cliente.nome}</TableCell>
                  <TableCell>{cliente.email ?? '—'}</TableCell>
                  <TableCell>{cliente.telefone ?? '—'}</TableCell>
                  <TableCell>{cliente.cpfCnpj ?? '—'}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      variant="outlined"
                      label={cliente.tipoPessoa === 'FISICA' ? 'Física' : 'Jurídica'}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={cliente.ativo ? 'success' : 'default'}
                      label={cliente.ativo ? 'Ativo' : 'Inativo'}
                    />
                  </TableCell>
                  {podeGerenciar && (
                    <TableCell align="right">
                      <Tooltip title="Editar">
                        <IconButton
                          size="small"
                          aria-label="Editar"
                          onClick={() => abrirEdicao(cliente)}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {cliente.ativo ? (
                        <Tooltip title="Desativar">
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Desativar"
                            onClick={() => setClienteParaDesativar(cliente)}
                          >
                            <PersonOffIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      ) : (
                        <Tooltip title="Reativar">
                          <IconButton
                            size="small"
                            color="success"
                            aria-label="Reativar"
                            onClick={() => reativar.mutate(cliente.id)}
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
                <TableCell colSpan={podeGerenciar ? 7 : 6} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Nenhum cliente encontrado.</Typography>
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

      <ClientFormDialog
        aberto={formAberto}
        cliente={clienteEmEdicao}
        onFechar={() => setFormAberto(false)}
      />

      <ConfirmDialog
        aberto={clienteParaDesativar !== null}
        titulo="Desativar cliente"
        mensagem={`O cliente "${clienteParaDesativar?.nome}" ficará inativo, mas o histórico de pedidos e orçamentos é preservado. Deseja continuar?`}
        rotuloConfirmar="Desativar"
        processando={desativar.isPending}
        onConfirmar={() => clienteParaDesativar && desativar.mutate(clienteParaDesativar.id)}
        onCancelar={() => setClienteParaDesativar(null)}
      />
    </Box>
  );
}
