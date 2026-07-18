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
import type { Role } from '../../api/types';
import { ROTULOS_ROLE, usersApi, type Usuario } from '../../api/users';
import { useAuth } from '../../auth/AuthContext';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useDebounce } from '../../hooks/useDebounce';
import { UserFormDialog } from './UserFormDialog';

const dataBr = (iso: string) => new Date(iso).toLocaleDateString('pt-BR');

const ROLES: Role[] = ['ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR', 'CLIENTE'];

export function UsersPage() {
  const { usuario: usuarioLogado } = useAuth();
  const queryClient = useQueryClient();

  const [busca, setBusca] = useState('');
  const [role, setRole] = useState<Role | ''>('');
  const [ativo, setAtivo] = useState<boolean | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const buscaEstavel = useDebounce(busca);

  const [formAberto, setFormAberto] = useState(false);
  const [usuarioEmEdicao, setUsuarioEmEdicao] = useState<Usuario | null>(null);
  const [usuarioParaDesativar, setUsuarioParaDesativar] = useState<Usuario | null>(null);

  const filtros = { busca: buscaEstavel, role, ativo, page, size };

  const { data, isPending, error } = useQuery({
    queryKey: ['users', filtros],
    queryFn: () => usersApi.listar(filtros),
    placeholderData: (anterior) => anterior,
  });

  const desativar = useMutation({
    mutationFn: (id: number) => usersApi.desativar(id),
    onSuccess: () => {
      toast.success('Usuário desativado. As sessões dele foram encerradas.');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setUsuarioParaDesativar(null);
    },
    onError: (erro) => {
      toast.error(mensagemDeErro(erro));
      setUsuarioParaDesativar(null);
    },
  });

  const reativar = useMutation({
    mutationFn: (id: number) => usersApi.reativar(id),
    onSuccess: (reativado) => {
      toast.success(`Usuário ${reativado.nome} reativado.`);
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  if (error) {
    return <Alert severity="error">{mensagemDeErro(error)}</Alert>;
  }

  const totalColunas = 6;

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
          Usuários
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            setUsuarioEmEdicao(null);
            setFormAberto(true);
          }}
        >
          Novo usuário
        </Button>
      </Box>

      <Paper sx={{ p: 2, mb: 2, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Buscar"
          placeholder="Nome ou e-mail"
          size="small"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value);
            setPage(0);
          }}
          sx={{ flexGrow: 1, minWidth: 220 }}
        />
        <TextField
          label="Papel"
          select
          size="small"
          value={role}
          onChange={(e) => {
            setRole(e.target.value as Role | '');
            setPage(0);
          }}
          slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          sx={{ minWidth: 170 }}
        >
          <option value="">Todos</option>
          {ROLES.map((valor) => (
            <option key={valor} value={valor}>
              {ROTULOS_ROLE[valor]}
            </option>
          ))}
        </TextField>
        <TextField
          label="Situação"
          select
          size="small"
          value={ativo === '' ? '' : String(ativo)}
          onChange={(e) => {
            setAtivo(e.target.value === '' ? '' : e.target.value === 'true');
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
              <TableCell>Papel</TableCell>
              <TableCell>Situação</TableCell>
              <TableCell>Criado em</TableCell>
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
              data.content.map((usuario) => (
                <TableRow key={usuario.id} hover>
                  <TableCell>{usuario.nome}</TableCell>
                  <TableCell>{usuario.email}</TableCell>
                  <TableCell>
                    <Chip size="small" variant="outlined" label={ROTULOS_ROLE[usuario.role]} />
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={usuario.ativo ? 'success' : 'default'}
                      label={usuario.ativo ? 'Ativo' : 'Inativo'}
                    />
                  </TableCell>
                  <TableCell>{dataBr(usuario.criadoEm)}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Editar">
                      <IconButton
                        size="small"
                        aria-label="Editar"
                        onClick={() => {
                          setUsuarioEmEdicao(usuario);
                          setFormAberto(true);
                        }}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {usuario.ativo ? (
                      <Tooltip
                        title={
                          usuario.id === usuarioLogado?.id
                            ? 'Você não pode desativar a si mesmo'
                            : 'Desativar'
                        }
                      >
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            aria-label="Desativar"
                            disabled={usuario.id === usuarioLogado?.id}
                            onClick={() => setUsuarioParaDesativar(usuario)}
                          >
                            <PersonOffIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    ) : (
                      <Tooltip title="Reativar">
                        <IconButton
                          size="small"
                          color="success"
                          aria-label="Reativar"
                          onClick={() => reativar.mutate(usuario.id)}
                        >
                          <RestoreIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={totalColunas} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Nenhum usuário encontrado.</Typography>
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

      <UserFormDialog
        aberto={formAberto}
        usuario={usuarioEmEdicao}
        onFechar={() => setFormAberto(false)}
      />

      <ConfirmDialog
        aberto={usuarioParaDesativar !== null}
        titulo="Desativar usuário"
        mensagem={`${usuarioParaDesativar?.nome} perderá o acesso imediatamente (as sessões são revogadas). O cadastro pode ser reativado depois. Deseja continuar?`}
        rotuloConfirmar="Desativar"
        processando={desativar.isPending}
        onConfirmar={() => usuarioParaDesativar && desativar.mutate(usuarioParaDesativar.id)}
        onCancelar={() => setUsuarioParaDesativar(null)}
      />
    </Box>
  );
}
