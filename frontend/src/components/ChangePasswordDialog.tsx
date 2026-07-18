import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../api/client';
import { usersApi } from '../api/users';

const esquemaSenha = z
  .object({
    senhaAtual: z.string().min(1, 'Informe a senha atual.'),
    novaSenha: z
      .string()
      .min(6, 'A nova senha deve ter entre 6 e 100 caracteres.')
      .max(100, 'A nova senha deve ter entre 6 e 100 caracteres.'),
    confirmacao: z.string(),
  })
  .refine((dados) => dados.novaSenha === dados.confirmacao, {
    path: ['confirmacao'],
    message: 'A confirmação não confere com a nova senha.',
  });

type DadosFormulario = z.infer<typeof esquemaSenha>;

const FORMULARIO_VAZIO: DadosFormulario = {
  senhaAtual: '',
  novaSenha: '',
  confirmacao: '',
};

interface ChangePasswordDialogProps {
  aberto: boolean;
  onFechar: () => void;
  /** Chamado após a troca — o backend revoga todas as sessões do usuário. */
  onSenhaTrocada: () => void;
}

export function ChangePasswordDialog({
  aberto,
  onFechar,
  onSenhaTrocada,
}: ChangePasswordDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaSenha),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(FORMULARIO_VAZIO);
    }
  }, [aberto, reset]);

  const trocar = useMutation({
    mutationFn: (dados: DadosFormulario) =>
      usersApi.trocarMinhaSenha(dados.senhaAtual, dados.novaSenha),
    onSuccess: () => {
      toast.success('Senha alterada. Entre novamente com a nova senha.');
      onSenhaTrocada();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Trocar minha senha</DialogTitle>
      <form onSubmit={handleSubmit((dados) => trocar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Stack spacing={2}>
            <Alert severity="info">
              Por segurança, todas as suas sessões são encerradas após a troca.
            </Alert>
            <TextField
              label="Senha atual *"
              type="password"
              autoComplete="current-password"
              error={!!errors.senhaAtual}
              helperText={errors.senhaAtual?.message}
              {...register('senhaAtual')}
            />
            <TextField
              label="Nova senha *"
              type="password"
              autoComplete="new-password"
              error={!!errors.novaSenha}
              helperText={errors.novaSenha?.message}
              {...register('novaSenha')}
            />
            <TextField
              label="Confirmar nova senha *"
              type="password"
              autoComplete="new-password"
              error={!!errors.confirmacao}
              helperText={errors.confirmacao?.message}
              {...register('confirmacao')}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onFechar} color="inherit" disabled={trocar.isPending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={trocar.isPending}>
            {trocar.isPending ? 'Trocando…' : 'Trocar senha'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
