import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import type { Role } from '../../api/types';
import { ROTULOS_ROLE, usersApi, type Usuario } from '../../api/users';

const ROLES: Role[] = ['ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR', 'CLIENTE'];

const esquemaUsuario = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(120, 'Máximo de 120 caracteres.'),
  email: z.email('E-mail inválido.').max(160, 'Máximo de 160 caracteres.'),
  senha: z.string(),
  role: z.enum(['ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR', 'CLIENTE']),
});

type DadosFormulario = z.infer<typeof esquemaUsuario>;

const FORMULARIO_VAZIO: DadosFormulario = {
  nome: '',
  email: '',
  senha: '',
  role: 'OPERADOR',
};

interface UserFormDialogProps {
  aberto: boolean;
  /** Usuário em edição; null = novo usuário. */
  usuario: Usuario | null;
  onFechar: () => void;
}

export function UserFormDialog({ aberto, usuario, onFechar }: UserFormDialogProps) {
  const queryClient = useQueryClient();
  const edicao = usuario !== null;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaUsuario),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(
        usuario
          ? { nome: usuario.nome, email: usuario.email, senha: '', role: usuario.role }
          : FORMULARIO_VAZIO,
      );
    }
  }, [aberto, usuario, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosFormulario) =>
      edicao
        ? usersApi.atualizar(usuario.id, {
            nome: dados.nome.trim(),
            email: dados.email.trim(),
            role: dados.role as Role,
          })
        : usersApi.criar({
            nome: dados.nome.trim(),
            email: dados.email.trim(),
            senha: dados.senha,
            role: dados.role as Role,
          }),
    onSuccess: () => {
      toast.success(edicao ? 'Usuário atualizado.' : 'Usuário cadastrado.');
      queryClient.invalidateQueries({ queryKey: ['users'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const enviar = handleSubmit((dados) => {
    // Senha só existe (e é obrigatória) no cadastro — a troca é do próprio usuário.
    if (!edicao && dados.senha.length < 6) {
      toast.error('A senha deve ter entre 6 e 100 caracteres.');
      return;
    }
    salvar.mutate(dados);
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="sm" fullWidth>
      <DialogTitle>{edicao ? `Editar usuário ${usuario.nome}` : 'Novo usuário'}</DialogTitle>
      <form onSubmit={enviar} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Stack spacing={2}>
            <TextField
              label="Nome *"
              error={!!errors.nome}
              helperText={errors.nome?.message}
              {...register('nome')}
            />
            <TextField
              label="E-mail *"
              type="email"
              error={!!errors.email}
              helperText={errors.email?.message}
              {...register('email')}
            />
            {!edicao && (
              <TextField
                label="Senha *"
                type="password"
                autoComplete="new-password"
                error={!!errors.senha}
                helperText={errors.senha?.message ?? 'Mínimo de 6 caracteres'}
                {...register('senha')}
              />
            )}
            <TextField
              label="Papel *"
              select
              slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
              {...register('role')}
            >
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {ROTULOS_ROLE[role]}
                </option>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onFechar} color="inherit" disabled={salvar.isPending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={salvar.isPending}>
            {salvar.isPending ? 'Salvando…' : 'Salvar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
