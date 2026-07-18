import { zodResolver } from '@hookform/resolvers/zod';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import {
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import { clientsApi, type Cliente } from '../../api/clients';
import { filamentsApi } from '../../api/filaments';
import { ordersApi, type Pedido, type PedidoInput } from '../../api/orders';
import {
  decimalObrigatorio,
  decimalOpcional,
  inteiroOpcional,
  paraCampo,
  paraNumero,
  paraTexto,
} from '../../lib/form';

const esquemaItem = z.object({
  id: z.number().nullable(),
  filamentoId: z.string(),
  nomePeca: z.string().trim().min(1, 'Informe a peça.').max(160, 'Máximo de 160 caracteres.'),
  descricao: z.string(),
  quantidade: inteiroOpcional(),
  pesoEstimadoG: decimalOpcional(),
  tempoImpressaoMinutos: inteiroOpcional(),
  precoUnitario: decimalObrigatorio('Preço inválido (use 0,00).'),
});

const esquemaPedido = z.object({
  clienteId: z
    .union([z.number(), z.null()])
    .refine((valor) => valor !== null, 'Selecione o cliente.'),
  dataEntregaPrevista: z.string(),
  desconto: decimalOpcional(),
  observacoes: z.string(),
  itens: z.array(esquemaItem).min(1, 'O pedido deve ter pelo menos um item.'),
});

// Input: o que o formulário edita (clienteId pode ser null antes da validação);
// Output: o que sai do zod após o refine (clienteId garantidamente number).
type DadosFormulario = z.input<typeof esquemaPedido>;
type DadosValidados = z.output<typeof esquemaPedido>;
type ItemFormulario = z.input<typeof esquemaItem>;

const ITEM_VAZIO: ItemFormulario = {
  id: null,
  filamentoId: '',
  nomePeca: '',
  descricao: '',
  quantidade: '1',
  pesoEstimadoG: '',
  tempoImpressaoMinutos: '',
  precoUnitario: '',
};

const FORMULARIO_VAZIO: DadosFormulario = {
  clienteId: null,
  dataEntregaPrevista: '',
  desconto: '',
  observacoes: '',
  itens: [ITEM_VAZIO],
};

function paraFormulario(pedido: Pedido): DadosFormulario {
  return {
    clienteId: pedido.clienteId,
    dataEntregaPrevista: pedido.dataEntregaPrevista ?? '',
    desconto: paraCampo(pedido.desconto),
    observacoes: pedido.observacoes ?? '',
    itens: pedido.itens.map((item) => ({
      id: item.id,
      // != null: o backend omite campos nulos do JSON (non_null).
      filamentoId: item.filamentoId != null ? String(item.filamentoId) : '',
      nomePeca: item.nomePeca,
      descricao: item.descricao ?? '',
      quantidade: String(item.quantidade),
      pesoEstimadoG: paraCampo(item.pesoEstimadoG),
      tempoImpressaoMinutos: paraCampo(item.tempoImpressaoMinutos),
      precoUnitario: paraCampo(item.precoUnitario),
    })),
  };
}

function paraPayload(dados: DadosValidados): PedidoInput {
  return {
    clienteId: dados.clienteId,
    dataEntregaPrevista: paraTexto(dados.dataEntregaPrevista),
    desconto: paraNumero(dados.desconto),
    observacoes: paraTexto(dados.observacoes),
    itens: dados.itens.map((item) => ({
      id: item.id,
      filamentoId: item.filamentoId === '' ? null : Number(item.filamentoId),
      nomePeca: item.nomePeca.trim(),
      descricao: paraTexto(item.descricao),
      quantidade: paraNumero(item.quantidade),
      pesoEstimadoG: paraNumero(item.pesoEstimadoG),
      tempoImpressaoMinutos: paraNumero(item.tempoImpressaoMinutos),
      precoUnitario: paraNumero(item.precoUnitario)!,
    })),
  };
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

interface OrderFormDialogProps {
  aberto: boolean;
  /** Id do pedido em edição/visualização; null = novo pedido. */
  pedidoId: number | null;
  onFechar: () => void;
}

export function OrderFormDialog({ aberto, pedidoId, onFechar }: OrderFormDialogProps) {
  const queryClient = useQueryClient();
  const [buscaCliente, setBuscaCliente] = useState('');
  // Objeto do Autocomplete em estado próprio: derivá-lo da query faz o value
  // oscilar para null durante o refetch e o MUI entra em loop de re-render.
  const [clienteSelecionado, setClienteSelecionado] = useState<Cliente | null>(null);

  const { data: pedido, isPending: carregandoPedido } = useQuery({
    queryKey: ['orders', 'detalhe', pedidoId],
    queryFn: () => ordersApi.buscarPorId(pedidoId!),
    enabled: aberto && pedidoId !== null,
  });

  // Só PENDENTE pode ser editado — nos demais status o diálogo é leitura.
  const somenteLeitura = pedidoId !== null && pedido !== undefined && pedido.status !== 'PENDENTE';
  const carregando = pedidoId !== null && carregandoPedido;

  const { data: clientes } = useQuery({
    queryKey: ['clients', 'select', buscaCliente],
    queryFn: () =>
      clientsApi.listar({ busca: buscaCliente, ativo: true, page: 0, size: 20 }),
    enabled: aberto,
    placeholderData: (anterior) => anterior,
  });

  const { data: filamentos } = useQuery({
    queryKey: ['filaments', 'select'],
    queryFn: () => filamentsApi.listar({ ativo: true, page: 0, size: 100 }),
    enabled: aberto,
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    formState: { errors },
  } = useForm<DadosFormulario, unknown, DadosValidados>({
    resolver: zodResolver(esquemaPedido),
    defaultValues: FORMULARIO_VAZIO,
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'itens' });

  useEffect(() => {
    if (aberto) {
      const edicao = pedido && pedidoId !== null;
      reset(edicao ? paraFormulario(pedido) : FORMULARIO_VAZIO);
      setClienteSelecionado(
        edicao ? ({ id: pedido.clienteId, nome: pedido.clienteNome } as Cliente) : null,
      );
    }
  }, [aberto, pedido, pedidoId, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosValidados) =>
      pedidoId !== null
        ? ordersApi.atualizar(pedidoId, paraPayload(dados))
        : ordersApi.criar(paraPayload(dados)),
    onSuccess: (salvo) => {
      toast.success(
        pedidoId !== null ? 'Pedido atualizado.' : `Pedido ${salvo.numero} criado.`,
      );
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const itens = watch('itens');
  const desconto = paraNumero(watch('desconto') || '') ?? 0;
  const subtotal = itens.reduce((soma, item) => {
    const quantidade = paraNumero(item.quantidade || '') ?? 1;
    const preco = paraNumero(item.precoUnitario || '') ?? 0;
    return soma + quantidade * preco;
  }, 0);
  const total = Math.max(subtotal - desconto, 0);

  const titulo =
    pedidoId === null
      ? 'Novo pedido'
      : somenteLeitura
        ? `Pedido ${pedido?.numero ?? ''} (${pedido ? pedido.status : ''})`
        : `Editar pedido ${pedido?.numero ?? ''}`;

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="lg" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <form onSubmit={handleSubmit((dados) => salvar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          {carregando ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
              <CircularProgress size={30} />
            </Box>
          ) : (
            <>
              <Box
                sx={{
                  display: 'grid',
                  gap: 2,
                  gridTemplateColumns: { xs: '1fr', sm: 'repeat(6, 1fr)' },
                }}
              >
                <Controller
                  name="clienteId"
                  control={control}
                  render={({ field }) => (
                    <Autocomplete
                      options={clientes?.content ?? []}
                      getOptionLabel={(opcao) => opcao.nome}
                      isOptionEqualToValue={(opcao, valor) => opcao.id === valor.id}
                      value={clienteSelecionado}
                      onChange={(_, novo) => {
                        field.onChange(novo ? novo.id : null);
                        setClienteSelecionado(novo);
                      }}
                      onInputChange={(_, texto) => setBuscaCliente(texto)}
                      disabled={somenteLeitura}
                      noOptionsText="Nenhum cliente encontrado"
                      renderInput={(params) => (
                        <TextField
                          {...params}
                          label="Cliente *"
                          error={!!errors.clienteId}
                          helperText={errors.clienteId?.message}
                        />
                      )}
                      sx={{ gridColumn: { sm: 'span 3' } }}
                    />
                  )}
                />
                <TextField
                  label="Entrega prevista"
                  type="date"
                  disabled={somenteLeitura}
                  slotProps={{ inputLabel: { shrink: true } }}
                  sx={{ gridColumn: { sm: 'span 1' } }}
                  {...register('dataEntregaPrevista')}
                />
                <TextField
                  label="Desconto (R$)"
                  disabled={somenteLeitura}
                  error={!!errors.desconto}
                  helperText={errors.desconto?.message}
                  sx={{ gridColumn: { sm: 'span 1' } }}
                  {...register('desconto')}
                />
                <TextField
                  label="Observações"
                  disabled={somenteLeitura}
                  sx={{ gridColumn: { sm: 'span 1' } }}
                  {...register('observacoes')}
                />
              </Box>

              <Divider sx={{ my: 2 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Itens do pedido
                </Typography>
              </Divider>
              {typeof errors.itens?.message === 'string' && (
                <Typography color="error" variant="body2" sx={{ mb: 1 }}>
                  {errors.itens.message}
                </Typography>
              )}

              {fields.map((campo, indice) => (
                <Box
                  key={campo.id}
                  sx={{
                    display: 'grid',
                    gap: 1.5,
                    gridTemplateColumns: { xs: '1fr', md: '3fr 2fr 1fr 1fr 1fr 1.4fr auto' },
                    alignItems: 'start',
                    mb: 1.5,
                    p: 1.5,
                    borderRadius: 1,
                    bgcolor: 'action.hover',
                  }}
                >
                  <TextField
                    label="Peça *"
                    size="small"
                    disabled={somenteLeitura}
                    error={!!errors.itens?.[indice]?.nomePeca}
                    helperText={errors.itens?.[indice]?.nomePeca?.message}
                    {...register(`itens.${indice}.nomePeca`)}
                  />
                  <TextField
                    label="Filamento"
                    size="small"
                    select
                    disabled={somenteLeitura}
                    defaultValue={campo.filamentoId}
                    slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                    {...register(`itens.${indice}.filamentoId`)}
                  >
                    <option value="">—</option>
                    {filamentos?.content.map((filamento) => (
                      <option key={filamento.id} value={String(filamento.id)}>
                        {filamento.nome}
                      </option>
                    ))}
                  </TextField>
                  <TextField
                    label="Qtde"
                    size="small"
                    disabled={somenteLeitura}
                    error={!!errors.itens?.[indice]?.quantidade}
                    helperText={errors.itens?.[indice]?.quantidade?.message}
                    {...register(`itens.${indice}.quantidade`)}
                  />
                  <TextField
                    label="Peso (g)"
                    size="small"
                    disabled={somenteLeitura}
                    error={!!errors.itens?.[indice]?.pesoEstimadoG}
                    helperText={errors.itens?.[indice]?.pesoEstimadoG?.message}
                    {...register(`itens.${indice}.pesoEstimadoG`)}
                  />
                  <TextField
                    label="Tempo (min)"
                    size="small"
                    disabled={somenteLeitura}
                    error={!!errors.itens?.[indice]?.tempoImpressaoMinutos}
                    helperText={errors.itens?.[indice]?.tempoImpressaoMinutos?.message}
                    {...register(`itens.${indice}.tempoImpressaoMinutos`)}
                  />
                  <TextField
                    label="Preço unit. (R$) *"
                    size="small"
                    disabled={somenteLeitura}
                    error={!!errors.itens?.[indice]?.precoUnitario}
                    helperText={errors.itens?.[indice]?.precoUnitario?.message}
                    {...register(`itens.${indice}.precoUnitario`)}
                  />
                  {!somenteLeitura && (
                    <Tooltip title="Remover item">
                      <span>
                        <IconButton
                          aria-label="Remover item"
                          onClick={() => remove(indice)}
                          disabled={fields.length === 1}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                  )}
                </Box>
              ))}

              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  mt: 2,
                  flexWrap: 'wrap',
                  gap: 1,
                }}
              >
                {!somenteLeitura ? (
                  <Button startIcon={<AddIcon />} onClick={() => append(ITEM_VAZIO)}>
                    Adicionar item
                  </Button>
                ) : (
                  <span />
                )}
                <Typography variant="subtitle1">
                  Subtotal: {moeda.format(subtotal)} — Desconto: {moeda.format(desconto)} —{' '}
                  <b>Total: {moeda.format(total)}</b>
                </Typography>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onFechar} color="inherit" disabled={salvar.isPending}>
            {somenteLeitura ? 'Fechar' : 'Cancelar'}
          </Button>
          {!somenteLeitura && (
            <Button type="submit" variant="contained" disabled={salvar.isPending || carregando}>
              {salvar.isPending ? 'Salvando…' : 'Salvar'}
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
}
