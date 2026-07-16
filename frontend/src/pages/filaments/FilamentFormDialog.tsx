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
import {
  filamentsApi,
  type Filamento,
  type FilamentoInput,
  type FilamentMaterial,
} from '../../api/filaments';
import {
  decimalObrigatorio,
  decimalOpcional,
  inteiroOpcional,
  paraCampo,
  paraNumero,
  paraTexto,
} from '../../lib/form';

const MATERIAIS: FilamentMaterial[] = [
  'PLA',
  'ABS',
  'PETG',
  'TPU',
  'ASA',
  'NYLON',
  'RESINA',
  'OUTRO',
];

const esquemaFilamento = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(120, 'Máximo de 120 caracteres.'),
  marca: z.union([z.literal(''), z.string().trim().max(80, 'Máximo de 80 caracteres.')]),
  material: z.enum(MATERIAIS),
  cor: z.union([z.literal(''), z.string().trim().max(40, 'Máximo de 40 caracteres.')]),
  diametroMm: decimalOpcional(),
  pesoBobinaG: decimalOpcional(),
  custoPorKg: decimalObrigatorio(),
  quantidadeEstoqueG: decimalOpcional(),
  estoqueMinimoG: decimalOpcional(),
  temperaturaBico: inteiroOpcional(),
  temperaturaMesa: inteiroOpcional(),
});

type DadosFormulario = z.infer<typeof esquemaFilamento>;

const FORMULARIO_VAZIO: DadosFormulario = {
  nome: '',
  marca: '',
  material: 'PLA',
  cor: '',
  diametroMm: '1,75',
  pesoBobinaG: '',
  custoPorKg: '',
  quantidadeEstoqueG: '',
  estoqueMinimoG: '',
  temperaturaBico: '',
  temperaturaMesa: '',
};

function paraFormulario(filamento: Filamento): DadosFormulario {
  return {
    nome: filamento.nome,
    marca: filamento.marca ?? '',
    material: filamento.material,
    cor: filamento.cor ?? '',
    diametroMm: paraCampo(filamento.diametroMm),
    pesoBobinaG: paraCampo(filamento.pesoBobinaG),
    custoPorKg: paraCampo(filamento.custoPorKg),
    quantidadeEstoqueG: paraCampo(filamento.quantidadeEstoqueG),
    estoqueMinimoG: paraCampo(filamento.estoqueMinimoG),
    temperaturaBico: paraCampo(filamento.temperaturaBico),
    temperaturaMesa: paraCampo(filamento.temperaturaMesa),
  };
}

function paraPayload(dados: DadosFormulario): FilamentoInput {
  return {
    nome: dados.nome.trim(),
    marca: paraTexto(dados.marca),
    material: dados.material,
    cor: paraTexto(dados.cor),
    diametroMm: paraNumero(dados.diametroMm),
    pesoBobinaG: paraNumero(dados.pesoBobinaG),
    custoPorKg: paraNumero(dados.custoPorKg),
    quantidadeEstoqueG: paraNumero(dados.quantidadeEstoqueG),
    estoqueMinimoG: paraNumero(dados.estoqueMinimoG),
    temperaturaBico: paraNumero(dados.temperaturaBico),
    temperaturaMesa: paraNumero(dados.temperaturaMesa),
  };
}

interface FilamentFormDialogProps {
  aberto: boolean;
  filamento: Filamento | null;
  onFechar: () => void;
}

export function FilamentFormDialog({ aberto, filamento, onFechar }: FilamentFormDialogProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaFilamento),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(filamento ? paraFormulario(filamento) : FORMULARIO_VAZIO);
    }
  }, [aberto, filamento, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosFormulario) =>
      filamento
        ? filamentsApi.atualizar(filamento.id, paraPayload(dados))
        : filamentsApi.criar(paraPayload(dados)),
    onSuccess: () => {
      toast.success(filamento ? 'Filamento atualizado.' : 'Filamento cadastrado.');
      queryClient.invalidateQueries({ queryKey: ['filaments'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="md" fullWidth>
      <DialogTitle>
        {filamento ? `Editar filamento — ${filamento.nome}` : 'Novo filamento'}
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
              sx={{ gridColumn: { sm: 'span 4' } }}
              {...register('nome')}
            />
            <TextField
              label="Material *"
              select
              defaultValue="PLA"
              slotProps={{ select: { native: true } }}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('material')}
            >
              {MATERIAIS.map((material) => (
                <option key={material} value={material}>
                  {material}
                </option>
              ))}
            </TextField>

            <TextField
              label="Marca"
              error={!!errors.marca}
              helperText={errors.marca?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('marca')}
            />
            <TextField
              label="Cor"
              error={!!errors.cor}
              helperText={errors.cor?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('cor')}
            />

            <TextField
              label="Custo por kg (R$) *"
              error={!!errors.custoPorKg}
              helperText={errors.custoPorKg?.message ?? 'Base de custo dos orçamentos'}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('custoPorKg')}
            />
            <TextField
              label="Diâmetro (mm)"
              error={!!errors.diametroMm}
              helperText={errors.diametroMm?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('diametroMm')}
            />
            <TextField
              label="Peso da bobina (g)"
              error={!!errors.pesoBobinaG}
              helperText={errors.pesoBobinaG?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('pesoBobinaG')}
            />

            {!filamento && (
              <TextField
                label="Estoque inicial (g)"
                error={!!errors.quantidadeEstoqueG}
                helperText={
                  errors.quantidadeEstoqueG?.message ??
                  'Depois, o estoque muda só por movimentação'
                }
                sx={{ gridColumn: { sm: 'span 3' } }}
                {...register('quantidadeEstoqueG')}
              />
            )}
            <TextField
              label="Estoque mínimo (g)"
              error={!!errors.estoqueMinimoG}
              helperText={errors.estoqueMinimoG?.message ?? 'Alerta de estoque baixo'}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('estoqueMinimoG')}
            />

            <Typography
              variant="subtitle2"
              color="text.secondary"
              sx={{ gridColumn: '1 / -1', mt: 1 }}
            >
              Temperaturas recomendadas (°C)
            </Typography>
            <TextField
              label="Bico"
              error={!!errors.temperaturaBico}
              helperText={errors.temperaturaBico?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('temperaturaBico')}
            />
            <TextField
              label="Mesa"
              error={!!errors.temperaturaMesa}
              helperText={errors.temperaturaMesa?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('temperaturaMesa')}
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
