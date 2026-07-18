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
  quotesApi,
  ROTULOS_STATUS_ORCAMENTO,
  TRANSICOES_ORCAMENTO,
  type Orcamento,
  type QuoteStatus,
} from '../../api/quotes';

interface QuoteStatusDialogProps {
  orcamento: Orcamento | null;
  onFechar: () => void;
}

/** Move o orçamento no ciclo RASCUNHO↔ENVIADO→APROVADO/REJEITADO/EXPIRADO. */
export function QuoteStatusDialog({ orcamento, onFechar }: QuoteStatusDialogProps) {
  const queryClient = useQueryClient();
  const opcoes = orcamento ? TRANSICOES_ORCAMENTO[orcamento.status] : [];
  const [status, setStatus] = useState<QuoteStatus | ''>('');

  useEffect(() => {
    if (orcamento) {
      setStatus(TRANSICOES_ORCAMENTO[orcamento.status][0] ?? '');
    }
  }, [orcamento]);

  const alterar = useMutation({
    mutationFn: () => quotesApi.alterarStatus(orcamento!.id, status as QuoteStatus),
    onSuccess: (atualizado) => {
      toast.success(
        `Orçamento ${atualizado.numero} agora está ${
          ROTULOS_STATUS_ORCAMENTO[atualizado.status]
        }.`,
      );
      queryClient.invalidateQueries({ queryKey: ['quotes'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={orcamento !== null} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Mudar status do orçamento</DialogTitle>
      <DialogContent sx={{ pt: 1 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {orcamento?.numero} — status atual:{' '}
          <b>{orcamento ? ROTULOS_STATUS_ORCAMENTO[orcamento.status] : ''}</b>
        </Typography>
        {opcoes.length === 0 ? (
          <Alert severity="info">
            Este orçamento está em um status final. Aprovados são convertidos em pedido
            pela ação “Converter”.
          </Alert>
        ) : (
          <TextField
            label="Novo status"
            select
            fullWidth
            value={status}
            onChange={(e) => setStatus(e.target.value as QuoteStatus)}
            slotProps={{ select: { native: true } }}
            helperText={
              status === 'ENVIADO'
                ? 'Enviado libera o link público de aprovação do cliente.'
                : status === 'RASCUNHO'
                  ? 'Voltar a rascunho permite editar o orçamento.'
                  : undefined
            }
          >
            {opcoes.map((opcao) => (
              <option key={opcao} value={opcao}>
                {ROTULOS_STATUS_ORCAMENTO[opcao]}
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
