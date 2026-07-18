import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import { filamentsApi } from '../../api/filaments';
import { ordersApi } from '../../api/orders';
import { printersApi } from '../../api/printers';
import { printsApi, type ImpressaoStartInput } from '../../api/prints';
import { paraTexto } from '../../lib/form';

const esquemaInicio = z.object({
  impressoraId: z.string().min(1, 'Selecione a impressora.'),
  filamentoId: z.string(),
  pedidoId: z.string(),
  itemPedidoId: z.string(),
  observacoes: z.string(),
});

type DadosFormulario = z.infer<typeof esquemaInicio>;

const FORMULARIO_VAZIO: DadosFormulario = {
  impressoraId: '',
  filamentoId: '',
  pedidoId: '',
  itemPedidoId: '',
  observacoes: '',
};

function paraPayload(dados: DadosFormulario): ImpressaoStartInput {
  return {
    impressoraId: Number(dados.impressoraId),
    filamentoId: dados.filamentoId === '' ? null : Number(dados.filamentoId),
    itemPedidoId: dados.itemPedidoId === '' ? null : Number(dados.itemPedidoId),
    observacoes: paraTexto(dados.observacoes),
  };
}

interface PrintStartDialogProps {
  aberto: boolean;
  onFechar: () => void;
}

/** Inicia um job: impressora DISPONIVEL, filamento opcional e item de pedido opcional. */
export function PrintStartDialog({ aberto, onFechar }: PrintStartDialogProps) {
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaInicio),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      reset(FORMULARIO_VAZIO);
    }
  }, [aberto, reset]);

  const { data: impressoras } = useQuery({
    queryKey: ['printers', 'select', 'disponiveis'],
    queryFn: () =>
      printersApi.listar({ status: 'DISPONIVEL', ativo: true, page: 0, size: 100 }),
    enabled: aberto,
  });

  const { data: filamentos } = useQuery({
    queryKey: ['filaments', 'select'],
    queryFn: () => filamentsApi.listar({ ativo: true, page: 0, size: 100 }),
    enabled: aberto,
  });

  // Só pedidos EM_PRODUCAO podem ter itens impressos.
  const { data: pedidos } = useQuery({
    queryKey: ['orders', 'select', 'em-producao'],
    queryFn: () => ordersApi.listar({ status: 'EM_PRODUCAO', page: 0, size: 100 }),
    enabled: aberto,
  });

  const pedidoId = watch('pedidoId');
  const { data: pedidoDetalhe } = useQuery({
    queryKey: ['orders', 'detalhe', Number(pedidoId)],
    queryFn: () => ordersApi.buscarPorId(Number(pedidoId)),
    enabled: aberto && pedidoId !== '',
  });

  const iniciar = useMutation({
    mutationFn: (dados: DadosFormulario) => printsApi.iniciar(paraPayload(dados)),
    onSuccess: () => {
      toast.success('Impressão iniciada.');
      queryClient.invalidateQueries({ queryKey: ['prints'] });
      queryClient.invalidateQueries({ queryKey: ['printers'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="sm" fullWidth>
      <DialogTitle>Nova impressão</DialogTitle>
      <form onSubmit={handleSubmit((dados) => iniciar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          <Stack spacing={2}>
            <TextField
              label="Impressora *"
              select
              error={!!errors.impressoraId}
              helperText={errors.impressoraId?.message ?? 'Somente impressoras disponíveis'}
              slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
              {...register('impressoraId')}
            >
              <option value="">—</option>
              {impressoras?.content.map((impressora) => (
                <option key={impressora.id} value={String(impressora.id)}>
                  {impressora.nome}
                </option>
              ))}
            </TextField>
            <TextField
              label="Filamento"
              select
              helperText="Necessário para abater o estoque ao finalizar"
              slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
              {...register('filamentoId')}
            >
              <option value="">—</option>
              {filamentos?.content.map((filamento) => (
                <option key={filamento.id} value={String(filamento.id)}>
                  {filamento.nome}
                </option>
              ))}
            </TextField>
            <TextField
              label="Pedido (em produção)"
              select
              helperText="Vazio = impressão avulsa"
              slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
              {...register('pedidoId', {
                onChange: () => setValue('itemPedidoId', ''),
              })}
            >
              <option value="">—</option>
              {pedidos?.content.map((pedido) => (
                <option key={pedido.id} value={String(pedido.id)}>
                  {pedido.numero} — {pedido.clienteNome}
                </option>
              ))}
            </TextField>
            {pedidoId !== '' && (
              <TextField
                label="Item do pedido"
                select
                slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                {...register('itemPedidoId')}
              >
                <option value="">—</option>
                {pedidoDetalhe?.itens.map((item) => (
                  <option key={item.id} value={String(item.id)}>
                    {item.nomePeca} (x{item.quantidade})
                  </option>
                ))}
              </TextField>
            )}
            <TextField label="Observações" multiline minRows={2} {...register('observacoes')} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onFechar} color="inherit" disabled={iniciar.isPending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={iniciar.isPending}>
            {iniciar.isPending ? 'Iniciando…' : 'Iniciar impressão'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
