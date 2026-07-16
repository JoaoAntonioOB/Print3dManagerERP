import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import { filamentsApi, type Filamento } from '../../api/filaments';
import { decimalObrigatorio, paraNumero } from '../../lib/form';

const esquemaMovimento = z.object({
  tipo: z.enum(['ENTRADA', 'SAIDA']),
  quantidadeG: decimalObrigatorio('Quantidade inválida (use gramas, ex.: 250,5).'),
});

type DadosMovimento = z.infer<typeof esquemaMovimento>;

interface FilamentStockDialogProps {
  filamento: Filamento | null;
  onFechar: () => void;
}

/** Movimentação manual de estoque em gramas (reposição, ajuste, perda). */
export function FilamentStockDialog({ filamento, onFechar }: FilamentStockDialogProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosMovimento>({
    resolver: zodResolver(esquemaMovimento),
    defaultValues: { tipo: 'ENTRADA', quantidadeG: '' },
  });

  useEffect(() => {
    if (filamento) {
      reset({ tipo: 'ENTRADA', quantidadeG: '' });
    }
  }, [filamento, reset]);

  const movimentar = useMutation({
    mutationFn: (dados: DadosMovimento) =>
      filamentsApi.movimentarEstoque(
        filamento!.id,
        dados.tipo,
        paraNumero(dados.quantidadeG)!,
      ),
    onSuccess: (atualizado) => {
      toast.success(
        `Estoque atualizado: ${atualizado.quantidadeEstoqueG} g de ${atualizado.nome}.`,
      );
      queryClient.invalidateQueries({ queryKey: ['filaments'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={filamento !== null} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Movimentar estoque</DialogTitle>
      <form onSubmit={handleSubmit((dados) => movimentar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {filamento?.nome} — saldo atual: <b>{filamento?.quantidadeEstoqueG} g</b>
          </Typography>
          <Stack spacing={2}>
            <TextField
              label="Tipo *"
              select
              defaultValue="ENTRADA"
              slotProps={{ select: { native: true } }}
              {...register('tipo')}
            >
              <option value="ENTRADA">Entrada (reposição)</option>
              <option value="SAIDA">Saída (ajuste/perda)</option>
            </TextField>
            <TextField
              label="Quantidade (g) *"
              autoFocus
              error={!!errors.quantidadeG}
              helperText={errors.quantidadeG?.message}
              {...register('quantidadeG')}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onFechar} color="inherit" disabled={movimentar.isPending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={movimentar.isPending}>
            {movimentar.isPending ? 'Aplicando…' : 'Aplicar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
