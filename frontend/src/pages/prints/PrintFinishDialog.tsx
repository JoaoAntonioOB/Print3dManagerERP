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
import { printsApi, type Impressao } from '../../api/prints';
import { decimalOpcional, paraNumero, paraTexto } from '../../lib/form';

const esquemaFim = z.object({
  motivoFalha: z.string(),
  pesoUtilizadoG: decimalOpcional('Peso inválido (use 0,00).'),
  consumoEnergiaKwh: decimalOpcional('Consumo inválido (use 0,00).'),
  observacoes: z.string(),
});

type DadosFormulario = z.infer<typeof esquemaFim>;

const FORMULARIO_VAZIO: DadosFormulario = {
  motivoFalha: '',
  pesoUtilizadoG: '',
  consumoEnergiaKwh: '',
  observacoes: '',
};

export type ModoFinalizacao = 'concluir' | 'falhar';

interface PrintFinishDialogProps {
  impressao: Impressao | null;
  modo: ModoFinalizacao;
  onFechar: () => void;
}

/**
 * Conclui ou registra falha de um job EM_ANDAMENTO. Nos dois casos o peso
 * informado é abatido do estoque do filamento (na falha, material perdido).
 */
export function PrintFinishDialog({ impressao, modo, onFechar }: PrintFinishDialogProps) {
  const queryClient = useQueryClient();
  const concluindo = modo === 'concluir';

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaFim),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (impressao) {
      reset(FORMULARIO_VAZIO);
    }
  }, [impressao, reset]);

  const finalizar = useMutation({
    mutationFn: (dados: DadosFormulario) => {
      const corpo = {
        pesoUtilizadoG: paraNumero(dados.pesoUtilizadoG),
        consumoEnergiaKwh: paraNumero(dados.consumoEnergiaKwh),
        observacoes: paraTexto(dados.observacoes),
      };
      return concluindo
        ? printsApi.concluir(impressao!.id, corpo)
        : printsApi.falhar(impressao!.id, { ...corpo, motivoFalha: dados.motivoFalha.trim() });
    },
    onSuccess: () => {
      toast.success(concluindo ? 'Impressão concluída.' : 'Falha registrada.');
      queryClient.invalidateQueries({ queryKey: ['prints'] });
      queryClient.invalidateQueries({ queryKey: ['printers'] });
      queryClient.invalidateQueries({ queryKey: ['filaments'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const enviar = handleSubmit((dados) => {
    if (!concluindo && dados.motivoFalha.trim() === '') {
      toast.error('Informe o motivo da falha.');
      return;
    }
    finalizar.mutate(dados);
  });

  return (
    <Dialog open={impressao !== null} onClose={onFechar} maxWidth="sm" fullWidth>
      <DialogTitle>{concluindo ? 'Concluir impressão' : 'Registrar falha'}</DialogTitle>
      <form onSubmit={enviar} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {impressao?.impressoraNome}
            {impressao?.filamentoNome ? ` — ${impressao.filamentoNome}` : ''} — a impressora
            volta a ficar disponível.
          </Typography>
          <Stack spacing={2}>
            {!concluindo && (
              <TextField
                label="Motivo da falha *"
                error={!!errors.motivoFalha}
                helperText={errors.motivoFalha?.message}
                {...register('motivoFalha')}
              />
            )}
            <TextField
              label={concluindo ? 'Peso consumido (g)' : 'Peso desperdiçado (g)'}
              error={!!errors.pesoUtilizadoG}
              helperText={
                errors.pesoUtilizadoG?.message ??
                (impressao?.filamentoNome
                  ? `Abatido do estoque de ${impressao.filamentoNome}`
                  : 'Sem filamento no job, não movimenta estoque')
              }
              {...register('pesoUtilizadoG')}
            />
            <TextField
              label="Energia consumida (kWh)"
              error={!!errors.consumoEnergiaKwh}
              helperText={errors.consumoEnergiaKwh?.message}
              {...register('consumoEnergiaKwh')}
            />
            <TextField label="Observações" multiline minRows={2} {...register('observacoes')} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onFechar} color="inherit" disabled={finalizar.isPending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            color={concluindo ? 'primary' : 'error'}
            disabled={finalizar.isPending}
          >
            {finalizar.isPending
              ? 'Salvando…'
              : concluindo
                ? 'Concluir'
                : 'Registrar falha'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
