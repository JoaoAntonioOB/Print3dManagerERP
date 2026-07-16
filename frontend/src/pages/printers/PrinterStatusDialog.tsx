import {
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
import { printersApi, type Impressora, type PrinterStatus } from '../../api/printers';

const OPCOES: { valor: PrinterStatus; rotulo: string }[] = [
  { valor: 'DISPONIVEL', rotulo: 'Disponível' },
  { valor: 'IMPRIMINDO', rotulo: 'Imprimindo' },
  { valor: 'EM_MANUTENCAO', rotulo: 'Em manutenção' },
];

interface PrinterStatusDialogProps {
  impressora: Impressora | null;
  onFechar: () => void;
}

/** Mudança manual da situação operacional (jobs de impressão também a alteram). */
export function PrinterStatusDialog({ impressora, onFechar }: PrinterStatusDialogProps) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<PrinterStatus>('DISPONIVEL');

  useEffect(() => {
    if (impressora) {
      setStatus(impressora.status === 'INATIVA' ? 'DISPONIVEL' : impressora.status);
    }
  }, [impressora]);

  const alterar = useMutation({
    mutationFn: () => printersApi.alterarStatus(impressora!.id, status),
    onSuccess: (atualizada) => {
      toast.success(`Status de ${atualizada.nome} atualizado.`);
      queryClient.invalidateQueries({ queryKey: ['printers'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={impressora !== null} onClose={onFechar} maxWidth="xs" fullWidth>
      <DialogTitle>Mudar status</DialogTitle>
      <DialogContent sx={{ pt: 1 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {impressora?.nome}
        </Typography>
        <TextField
          label="Novo status"
          select
          fullWidth
          value={status}
          onChange={(e) => setStatus(e.target.value as PrinterStatus)}
          slotProps={{ select: { native: true } }}
        >
          {OPCOES.map((opcao) => (
            <option key={opcao.valor} value={opcao.valor}>
              {opcao.rotulo}
            </option>
          ))}
        </TextField>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onFechar} color="inherit" disabled={alterar.isPending}>
          Cancelar
        </Button>
        <Button variant="contained" onClick={() => alterar.mutate()} disabled={alterar.isPending}>
          {alterar.isPending ? 'Aplicando…' : 'Aplicar'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
