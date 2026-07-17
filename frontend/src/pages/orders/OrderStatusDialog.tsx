import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { mensagemDeErro } from '../../api/client';
import {
  ordersApi,
  ROTULOS_STATUS_PEDIDO,
  TRANSICOES_PEDIDO,
  type OrderStatus,
  type PedidoResumo,
} from '../../api/orders';

interface OrderStatusDialogProps {
  pedido: PedidoResumo | null;
  onFechar: () => void;
}

/** Avança/cancela o pedido seguindo a máquina de estados do backend. */
export function OrderStatusDialog({ pedido, onFechar }: OrderStatusDialogProps) {
  const queryClient = useQueryClient();
  const opcoes = pedido ? TRANSICOES_PEDIDO[pedido.status] : [];
  const [status, setStatus] = useState<OrderStatus | ''>('');

  useEffect(() => {
    if (pedido) {
      setStatus(TRANSICOES_PEDIDO[pedido.status][0] ?? '');
    }
  }, [pedido]);

  const alterar = useMutation({
    mutationFn: () => ordersApi.alterarStatus(pedido!.id, status as OrderStatus),
    onSuccess: (atualizado) => {
      toast.success(
        `Pedido ${atualizado.numero} agora está ${ROTULOS_STATUS_PEDIDO[atualizado.status]}.`,
      );
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={pedido !== null} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Mudar status do pedido</DialogTitle>
      <DialogContent sx={{ pt: 1 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {pedido?.numero} — status atual:{' '}
          <b>{pedido ? ROTULOS_STATUS_PEDIDO[pedido.status] : ''}</b>
        </Typography>
        {opcoes.length === 0 ? (
          <Alert severity="info">
            Este pedido está em um status final e não pode mais mudar.
          </Alert>
        ) : (
          <TextField
            label="Novo status"
            select
            fullWidth
            value={status}
            onChange={(e) => setStatus(e.target.value as OrderStatus)}
            slotProps={{ select: { native: true } }}
            helperText={
              status === 'ENTREGUE'
                ? 'A entrega registra a data e gera a receita no Financeiro.'
                : undefined
            }
          >
            {opcoes.map((opcao) => (
              <option key={opcao} value={opcao}>
                {ROTULOS_STATUS_PEDIDO[opcao]}
              </option>
            ))}
          </TextField>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onFechar} color="inherit" disabled={alterar.isPending}>
          Cancelar
        </Button>
        {opcoes.length > 0 && (
          <Button
            variant="contained"
            onClick={() => alterar.mutate()}
            disabled={alterar.isPending || status === ''}
          >
            {alterar.isPending ? 'Aplicando…' : 'Aplicar'}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
