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
import {
  financialApi,
  type Transacao,
  type TransacaoInput,
  type TransactionStatus,
  type TransactionType,
} from '../../api/financial';
import { decimalObrigatorio, paraCampo, paraNumero, paraTexto } from '../../lib/form';

const esquemaTransacao = z.object({
  tipo: z.enum(['RECEITA', 'DESPESA']),
  categoria: z.string().trim().min(1, 'Informe a categoria.').max(60, 'Máximo de 60 caracteres.'),
  descricao: z.string().trim().min(1, 'Informe a descrição.').max(255, 'Máximo de 255 caracteres.'),
  valor: decimalObrigatorio('Valor inválido (use 0,00).'),
  dataTransacao: z.string().min(1, 'Informe a data.'),
  status: z.enum(['PENDENTE', 'PAGA']),
  formaPagamento: z.string().max(30, 'Máximo de 30 caracteres.'),
  observacoes: z.string(),
});

type DadosFormulario = z.infer<typeof esquemaTransacao>;

const hoje = () => new Date().toISOString().slice(0, 10);

const FORMULARIO_VAZIO: DadosFormulario = {
  tipo: 'DESPESA',
  categoria: '',
  descricao: '',
  valor: '',
  dataTransacao: hoje(),
  status: 'PENDENTE',
  formaPagamento: '',
  observacoes: '',
};

function paraFormulario(transacao: Transacao): DadosFormulario {
  return {
    tipo: transacao.tipo,
    categoria: transacao.categoria,
    descricao: transacao.descricao,
    valor: paraCampo(transacao.valor),
    dataTransacao: transacao.dataTransacao,
    status: 'PENDENTE',
    formaPagamento: transacao.formaPagamento ?? '',
    observacoes: transacao.observacoes ?? '',
  };
}

interface TransactionFormDialogProps {
  aberto: boolean;
  /** Transação em edição (sempre PENDENTE); null = novo lançamento. */
  transacao: Transacao | null;
  onFechar: () => void;
}

export function TransactionFormDialog({
  aberto,
  transacao,
  onFechar,
}: TransactionFormDialogProps) {
  const queryClient = useQueryClient();
  const edicao = transacao !== null;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaTransacao),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(transacao ? paraFormulario(transacao) : { ...FORMULARIO_VAZIO, dataTransacao: hoje() });
    }
  }, [aberto, transacao, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosFormulario) => {
      // Vínculos de pedido/cliente são preservados na edição (o form não os altera).
      const corpo: TransacaoInput = {
        tipo: dados.tipo as TransactionType,
        categoria: dados.categoria.trim(),
        descricao: dados.descricao.trim(),
        valor: paraNumero(dados.valor)!,
        dataTransacao: dados.dataTransacao,
        status: edicao ? undefined : (dados.status as TransactionStatus),
        formaPagamento: paraTexto(dados.formaPagamento),
        pedidoId: transacao?.pedidoId ?? null,
        clienteId: transacao?.clienteId ?? null,
        observacoes: paraTexto(dados.observacoes),
      };
      return edicao ? financialApi.atualizar(transacao.id, corpo) : financialApi.criar(corpo);
    },
    onSuccess: () => {
      toast.success(edicao ? 'Transação atualizada.' : 'Transação lançada.');
      queryClient.invalidateQueries({ queryKey: ['financial'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="sm" fullWidth>
      <DialogTitle>{edicao ? 'Editar transação' : 'Nova transação'}</DialogTitle>
      <form onSubmit={handleSubmit((dados) => salvar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
            }}
          >
            <TextField
              label="Tipo *"
              select
              slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
              {...register('tipo')}
            >
              <option value="DESPESA">Despesa</option>
              <option value="RECEITA">Receita</option>
            </TextField>
            <TextField
              label="Categoria *"
              error={!!errors.categoria}
              helperText={errors.categoria?.message}
              {...register('categoria')}
            />
            <TextField
              label="Descrição *"
              error={!!errors.descricao}
              helperText={errors.descricao?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('descricao')}
            />
            <TextField
              label="Valor (R$) *"
              error={!!errors.valor}
              helperText={errors.valor?.message ?? 'Sempre positivo — o sinal vem do tipo'}
              {...register('valor')}
            />
            <TextField
              label="Data *"
              type="date"
              error={!!errors.dataTransacao}
              helperText={errors.dataTransacao?.message}
              slotProps={{ inputLabel: { shrink: true } }}
              {...register('dataTransacao')}
            />
            {!edicao && (
              <TextField
                label="Situação inicial"
                select
                slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                {...register('status')}
              >
                <option value="PENDENTE">Pendente</option>
                <option value="PAGA">Paga</option>
              </TextField>
            )}
            <TextField
              label="Forma de pagamento"
              error={!!errors.formaPagamento}
              helperText={errors.formaPagamento?.message}
              {...register('formaPagamento')}
            />
            <TextField
              label="Observações"
              sx={{ gridColumn: { sm: 'span 2' } }}
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
