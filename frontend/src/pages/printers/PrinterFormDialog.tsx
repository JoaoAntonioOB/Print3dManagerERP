import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import { printersApi, type Impressora, type ImpressoraInput } from '../../api/printers';
import {
  decimalOpcional,
  inteiroOpcional,
  paraCampo,
  paraNumero,
  paraTexto,
} from '../../lib/form';

const esquemaImpressora = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(120, 'Máximo de 120 caracteres.'),
  marca: z.union([z.literal(''), z.string().trim().max(80, 'Máximo de 80 caracteres.')]),
  modelo: z.union([z.literal(''), z.string().trim().max(80, 'Máximo de 80 caracteres.')]),
  potenciaWatts: inteiroOpcional(),
  volumeXMm: inteiroOpcional(),
  volumeYMm: inteiroOpcional(),
  volumeZMm: inteiroOpcional(),
  valorAquisicao: decimalOpcional(),
  dataAquisicao: z.string(),
  observacoes: z.string(),
});

type DadosFormulario = z.infer<typeof esquemaImpressora>;

const FORMULARIO_VAZIO: DadosFormulario = {
  nome: '',
  marca: '',
  modelo: '',
  potenciaWatts: '',
  volumeXMm: '',
  volumeYMm: '',
  volumeZMm: '',
  valorAquisicao: '',
  dataAquisicao: '',
  observacoes: '',
};

function paraFormulario(impressora: Impressora): DadosFormulario {
  return {
    nome: impressora.nome,
    marca: impressora.marca ?? '',
    modelo: impressora.modelo ?? '',
    potenciaWatts: paraCampo(impressora.potenciaWatts),
    volumeXMm: paraCampo(impressora.volumeXMm),
    volumeYMm: paraCampo(impressora.volumeYMm),
    volumeZMm: paraCampo(impressora.volumeZMm),
    valorAquisicao: paraCampo(impressora.valorAquisicao),
    dataAquisicao: impressora.dataAquisicao ?? '',
    observacoes: impressora.observacoes ?? '',
  };
}

function paraPayload(dados: DadosFormulario): ImpressoraInput {
  return {
    nome: dados.nome.trim(),
    marca: paraTexto(dados.marca),
    modelo: paraTexto(dados.modelo),
    potenciaWatts: paraNumero(dados.potenciaWatts),
    volumeXMm: paraNumero(dados.volumeXMm),
    volumeYMm: paraNumero(dados.volumeYMm),
    volumeZMm: paraNumero(dados.volumeZMm),
    valorAquisicao: paraNumero(dados.valorAquisicao),
    dataAquisicao: paraTexto(dados.dataAquisicao),
    observacoes: paraTexto(dados.observacoes),
  };
}

interface PrinterFormDialogProps {
  aberto: boolean;
  impressora: Impressora | null;
  onFechar: () => void;
}

export function PrinterFormDialog({ aberto, impressora, onFechar }: PrinterFormDialogProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaImpressora),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(impressora ? paraFormulario(impressora) : FORMULARIO_VAZIO);
    }
  }, [aberto, impressora, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosFormulario) =>
      impressora
        ? printersApi.atualizar(impressora.id, paraPayload(dados))
        : printersApi.criar(paraPayload(dados)),
    onSuccess: () => {
      toast.success(impressora ? 'Impressora atualizada.' : 'Impressora cadastrada.');
      queryClient.invalidateQueries({ queryKey: ['printers'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="md" fullWidth>
      <DialogTitle>
        {impressora ? `Editar impressora — ${impressora.nome}` : 'Nova impressora'}
      </DialogTitle>
      <form onSubmit={handleSubmit((dados) => salvar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(6, 1fr)' },
            }}
          >
            <TextField
              label="Nome *"
              autoFocus
              error={!!errors.nome}
              helperText={errors.nome?.message}
              sx={{ gridColumn: { sm: 'span 6' } }}
              {...register('nome')}
            />
            <TextField
              label="Marca"
              error={!!errors.marca}
              helperText={errors.marca?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('marca')}
            />
            <TextField
              label="Modelo"
              error={!!errors.modelo}
              helperText={errors.modelo?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('modelo')}
            />

            <TextField
              label="Potência (W)"
              error={!!errors.potenciaWatts}
              helperText={errors.potenciaWatts?.message ?? 'Usada no custo de energia'}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('potenciaWatts')}
            />
            <TextField
              label="Valor de aquisição (R$)"
              error={!!errors.valorAquisicao}
              helperText={errors.valorAquisicao?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('valorAquisicao')}
            />

            <Typography
              variant="subtitle2"
              color="text.secondary"
              sx={{ gridColumn: '1 / -1', mt: 1 }}
            >
              Volume de impressão (mm)
            </Typography>
            <TextField
              label="X"
              error={!!errors.volumeXMm}
              helperText={errors.volumeXMm?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('volumeXMm')}
            />
            <TextField
              label="Y"
              error={!!errors.volumeYMm}
              helperText={errors.volumeYMm?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('volumeYMm')}
            />
            <TextField
              label="Z"
              error={!!errors.volumeZMm}
              helperText={errors.volumeZMm?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('volumeZMm')}
            />

            <TextField
              label="Data de aquisição"
              type="date"
              slotProps={{ inputLabel: { shrink: true } }}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('dataAquisicao')}
            />
            <TextField
              label="Observações"
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('observacoes')}
            />
          </Box>
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
