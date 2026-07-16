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
import { inventoryApi, type ItemEstoque } from '../../api/inventory';
import { decimalObrigatorio, paraNumero } from '../../lib/form';

const esquemaMovimento = z.object({
  tipo: z.enum(['ENTRADA', 'SAIDA']),
  quantidade: decimalObrigatorio('Quantidade inválida (ex.: 2,5).'),
});

type DadosMovimento = z.infer<typeof esquemaMovimento>;

interface InventoryStockDialogProps {
  item: ItemEstoque | null;
  onFechar: () => void;
}

/** Movimentação manual de quantidade na unidade de medida do item. */
export function InventoryStockDialog({ item, onFechar }: InventoryStockDialogProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosMovimento>({
    resolver: zodResolver(esquemaMovimento),
    defaultValues: { tipo: 'ENTRADA', quantidade: '' },
  });

  useEffect(() => {
    if (item) {
      reset({ tipo: 'ENTRADA', quantidade: '' });
    }
  }, [item, reset]);

  const movimentar = useMutation({
    mutationFn: (dados: DadosMovimento) =>
      inventoryApi.movimentarEstoque(item!.id, dados.tipo, paraNumero(dados.quantidade)!),
    onSuccess: (atualizado) => {
      toast.success(
        `Estoque atualizado: ${atualizado.quantidade} ${atualizado.unidadeMedida} de ${atualizado.nome}.`,
      );
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={item !== null} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Movimentar estoque</DialogTitle>
      <form onSubmit={handleSubmit((dados) => movimentar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {item?.nome} — saldo atual:{' '}
            <b>
              {item?.quantidade} {item?.unidadeMedida}
            </b>
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
              <option value="SAIDA">Saída (consumo/ajuste)</option>
            </TextField>
            <TextField
              label={`Quantidade (${item?.unidadeMedida ?? ''}) *`}
              autoFocus
              error={!!errors.quantidade}
              helperText={errors.quantidade?.message}
              {...register('quantidade')}
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
