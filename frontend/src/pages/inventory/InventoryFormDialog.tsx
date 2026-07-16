import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import { inventoryApi, type ItemEstoque, type ItemEstoqueInput } from '../../api/inventory';
import { decimalOpcional, paraCampo, paraNumero, paraTexto } from '../../lib/form';

const esquemaItem = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(120, 'Máximo de 120 caracteres.'),
  descricao: z.string(),
  categoria: z.union([z.literal(''), z.string().trim().max(60, 'Máximo de 60 caracteres.')]),
  quantidade: decimalOpcional(),
  unidadeMedida: z.union([z.literal(''), z.string().trim().max(10, 'Máximo de 10 caracteres.')]),
  quantidadeMinima: decimalOpcional(),
  custoUnitario: decimalOpcional(),
  localizacao: z.union([z.literal(''), z.string().trim().max(80, 'Máximo de 80 caracteres.')]),
});

type DadosFormulario = z.infer<typeof esquemaItem>;

const FORMULARIO_VAZIO: DadosFormulario = {
  nome: '',
  descricao: '',
  categoria: '',
  quantidade: '',
  unidadeMedida: 'UN',
  quantidadeMinima: '',
  custoUnitario: '',
  localizacao: '',
};

function paraFormulario(item: ItemEstoque): DadosFormulario {
  return {
    nome: item.nome,
    descricao: item.descricao ?? '',
    categoria: item.categoria ?? '',
    quantidade: paraCampo(item.quantidade),
    unidadeMedida: item.unidadeMedida,
    quantidadeMinima: paraCampo(item.quantidadeMinima),
    custoUnitario: paraCampo(item.custoUnitario),
    localizacao: item.localizacao ?? '',
  };
}

function paraPayload(dados: DadosFormulario): ItemEstoqueInput {
  return {
    nome: dados.nome.trim(),
    descricao: paraTexto(dados.descricao),
    categoria: paraTexto(dados.categoria),
    quantidade: paraNumero(dados.quantidade),
    unidadeMedida: paraTexto(dados.unidadeMedida),
    quantidadeMinima: paraNumero(dados.quantidadeMinima),
    custoUnitario: paraNumero(dados.custoUnitario),
    localizacao: paraTexto(dados.localizacao),
  };
}

interface InventoryFormDialogProps {
  aberto: boolean;
  item: ItemEstoque | null;
  onFechar: () => void;
}

export function InventoryFormDialog({ aberto, item, onFechar }: InventoryFormDialogProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaItem),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(item ? paraFormulario(item) : FORMULARIO_VAZIO);
    }
  }, [aberto, item, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosFormulario) =>
      item
        ? inventoryApi.atualizar(item.id, paraPayload(dados))
        : inventoryApi.criar(paraPayload(dados)),
    onSuccess: () => {
      toast.success(item ? 'Item atualizado.' : 'Item cadastrado.');
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="md" fullWidth>
      <DialogTitle>{item ? `Editar item — ${item.nome}` : 'Novo item de estoque'}</DialogTitle>
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
              label="Categoria"
              error={!!errors.categoria}
              helperText={errors.categoria?.message ?? 'Ex.: Embalagem, Parafuso'}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('categoria')}
            />

            <TextField
              label="Descrição"
              sx={{ gridColumn: '1 / -1' }}
              {...register('descricao')}
            />

            {!item && (
              <TextField
                label="Quantidade inicial"
                error={!!errors.quantidade}
                helperText={
                  errors.quantidade?.message ?? 'Depois, muda só por movimentação'
                }
                sx={{ gridColumn: { sm: 'span 2' } }}
                {...register('quantidade')}
              />
            )}
            <TextField
              label="Unidade"
              error={!!errors.unidadeMedida}
              helperText={errors.unidadeMedida?.message ?? 'UN, KG, M, L…'}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('unidadeMedida')}
            />
            <TextField
              label="Quantidade mínima"
              error={!!errors.quantidadeMinima}
              helperText={errors.quantidadeMinima?.message ?? 'Alerta de estoque baixo'}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('quantidadeMinima')}
            />

            <TextField
              label="Custo unitário (R$)"
              error={!!errors.custoUnitario}
              helperText={errors.custoUnitario?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('custoUnitario')}
            />
            <TextField
              label="Localização"
              error={!!errors.localizacao}
              helperText={errors.localizacao?.message ?? 'Ex.: Prateleira A3'}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('localizacao')}
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
