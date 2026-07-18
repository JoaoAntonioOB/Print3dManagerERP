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
  financialApi,
  ROTULOS_STATUS_TRANSACAO,
  TRANSICOES_TRANSACAO,
  type Transacao,
  type TransactionStatus,
} from '../../api/financial';

interface TransactionStatusDialogProps {
  transacao: Transacao | null;
  onFechar: () => void;
}

/** Baixa (PAGA), estorno (volta a PENDENTE) ou cancelamento da transação. */
export function TransactionStatusDialog({ transacao, onFechar }: TransactionStatusDialogProps) {
  const queryClient = useQueryClient();
  const opcoes = transacao ? TRANSICOES_TRANSACAO[transacao.status] : [];
  const [status, setStatus] = useState<TransactionStatus | ''>('');

  useEffect(() => {
    if (transacao) {
      setStatus(TRANSICOES_TRANSACAO[transacao.status][0] ?? '');
    }
  }, [transacao]);

  const alterar = useMutation({
    mutationFn: () => financialApi.alterarStatus(transacao!.id, status as TransactionStatus),
    onSuccess: (atualizada) => {
      toast.success(
        `Transação agora está ${ROTULOS_STATUS_TRANSACAO[atualizada.status]}.`,
      );
      queryClient.invalidateQueries({ queryKey: ['financial'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={transacao !== null} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Mudar situação da transação</DialogTitle>
      <DialogContent sx={{ pt: 1 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {transacao?.descricao} — situação atual:{' '}
          <b>{transacao ? ROTULOS_STATUS_TRANSACAO[transacao.status] : ''}</b>
        </Typography>
        {opcoes.length === 0 ? (
          <Alert severity="info">Transações canceladas não mudam mais de situação.</Alert>
        ) : (
          <TextField
            label="Nova situação"
            select
            fullWidth
            value={status}
            onChange={(e) => setStatus(e.target.value as TransactionStatus)}
            slotProps={{ select: { native: true } }}
            helperText={
              status === 'PENDENTE'
                ? 'Estorna a baixa: a transação volta a ficar em aberto.'
                : status === 'CANCELADA'
                  ? 'Cancelada sai dos resumos e não pode mais mudar.'
                  : undefined
            }
          >
            {opcoes.map((opcao) => (
              <option key={opcao} value={opcao}>
                {ROTULOS_STATUS_TRANSACAO[opcao]}
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
