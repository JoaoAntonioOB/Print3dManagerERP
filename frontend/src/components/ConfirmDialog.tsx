import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';

interface ConfirmDialogProps {
  aberto: boolean;
  titulo: string;
  mensagem: string;
  rotuloConfirmar?: string;
  corConfirmar?: 'primary' | 'error' | 'warning';
  processando?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}

/** Diálogo de confirmação padrão para ações destrutivas (desativar, excluir…). */
export function ConfirmDialog({
  aberto,
  titulo,
  mensagem,
  rotuloConfirmar = 'Confirmar',
  corConfirmar = 'error',
  processando = false,
  onConfirmar,
  onCancelar,
}: ConfirmDialogProps) {
  return (
    <Dialog open={aberto} onClose={onCancelar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText>{mensagem}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancelar} color="inherit" disabled={processando}>
          Cancelar
        </Button>
        <Button
          onClick={onConfirmar}
          variant="contained"
          color={corConfirmar}
          disabled={processando}
        >
          {rotuloConfirmar}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
