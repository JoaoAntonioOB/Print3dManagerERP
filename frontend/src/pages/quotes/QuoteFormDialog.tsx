import { zodResolver } from '@hookform/resolvers/zod';
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
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import { clientsApi, type Cliente } from '../../api/clients';
import { filamentsApi } from '../../api/filaments';
import { printersApi } from '../../api/printers';
import {
  quotesApi,
  ROTULOS_STATUS_ORCAMENTO,
  type Orcamento,
  type OrcamentoInput,
} from '../../api/quotes';
import {
  decimalOpcional,
  inteiroOpcional,
  paraCampo,
  paraNumero,
  paraTexto,
} from '../../lib/form';

const esquemaOrcamento = z.object({
  clienteId: z
    .union([z.number(), z.null()])
    .refine((valor) => valor !== null, 'Selecione o cliente.'),
  impressoraId: z.string(),
  filamentoId: z.string(),
  descricao: z.string(),
  dataValidade: z.string(),
  tempoImpressaoMinutos: inteiroOpcional(),
  pesoEstimadoG: decimalOpcional(),
  markup: decimalOpcional('Markup inválido (use 0,00).'),
  precoFinal: decimalOpcional('Preço inválido (use 0,00).'),
  observacoes: z.string(),
});

// Input: o que o formulário edita (clienteId pode ser null antes da validação);
// Output: o que sai do zod após o refine (clienteId garantidamente number).
type DadosFormulario = z.input<typeof esquemaOrcamento>;
type DadosValidados = z.output<typeof esquemaOrcamento>;

const FORMULARIO_VAZIO: DadosFormulario = {
  clienteId: null,
  impressoraId: '',
  filamentoId: '',
  descricao: '',
  dataValidade: '',
  tempoImpressaoMinutos: '',
  pesoEstimadoG: '',
  markup: '',
  precoFinal: '',
  observacoes: '',
};

function paraFormulario(orcamento: Orcamento): DadosFormulario {
  return {
    clienteId: orcamento.clienteId,
    // != null de propósito: o backend omite campos nulos do JSON (non_null),
    // então valores ausentes chegam como undefined.
    impressoraId: orcamento.impressoraId != null ? String(orcamento.impressoraId) : '',
    filamentoId: orcamento.filamentoId != null ? String(orcamento.filamentoId) : '',
    descricao: orcamento.descricao ?? '',
    dataValidade: orcamento.dataValidade ?? '',
    tempoImpressaoMinutos: paraCampo(orcamento.tempoImpressaoMinutos),
    pesoEstimadoG: paraCampo(orcamento.pesoEstimadoG),
    markup: paraCampo(orcamento.markup),
    precoFinal: paraCampo(orcamento.precoFinal),
    observacoes: orcamento.observacoes ?? '',
  };
}

function paraPayload(dados: DadosValidados): OrcamentoInput {
  return {
    clienteId: dados.clienteId,
    impressoraId: dados.impressoraId === '' ? null : Number(dados.impressoraId),
    filamentoId: dados.filamentoId === '' ? null : Number(dados.filamentoId),
    descricao: paraTexto(dados.descricao),
    dataValidade: paraTexto(dados.dataValidade),
    tempoImpressaoMinutos: paraNumero(dados.tempoImpressaoMinutos),
    pesoEstimadoG: paraNumero(dados.pesoEstimadoG),
    markup: paraNumero(dados.markup),
    precoFinal: paraNumero(dados.precoFinal),
    observacoes: paraTexto(dados.observacoes),
  };
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** Linha rótulo→valor do quadro de custos calculados. */
function LinhaCusto({ rotulo, valor, destaque }: {
  rotulo: string;
  valor: string;
  destaque?: boolean;
}) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.25 }}>
      <Typography variant="body2" sx={{ fontWeight: destaque ? 700 : 400 }}>
        {rotulo}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: destaque ? 700 : 400 }}>
        {valor}
      </Typography>
    </Box>
  );
}

interface QuoteFormDialogProps {
  aberto: boolean;
  /** Id do orçamento em edição/visualização; null = novo orçamento. */
  orcamentoId: number | null;
  onFechar: () => void;
}

export function QuoteFormDialog({ aberto, orcamentoId, onFechar }: QuoteFormDialogProps) {
  const queryClient = useQueryClient();
  const [buscaCliente, setBuscaCliente] = useState('');
  // Objeto do Autocomplete em estado próprio: derivá-lo da query faz o value
  // oscilar para null durante o refetch e o MUI entra em loop de re-render.
  const [clienteSelecionado, setClienteSelecionado] = useState<Cliente | null>(null);

  const { data: orcamento, isPending: carregandoOrcamento } = useQuery({
    queryKey: ['quotes', 'detalhe', orcamentoId],
    queryFn: () => quotesApi.buscarPorId(orcamentoId!),
    enabled: aberto && orcamentoId !== null,
  });

  // Só RASCUNHO pode ser editado — nos demais status o diálogo é leitura.
  const somenteLeitura =
    orcamentoId !== null && orcamento !== undefined && orcamento.status !== 'RASCUNHO';
  const carregando = orcamentoId !== null && carregandoOrcamento;

  const { data: clientes } = useQuery({
    queryKey: ['clients', 'select', buscaCliente],
    queryFn: () =>
      clientsApi.listar({ busca: buscaCliente, ativo: true, page: 0, size: 20 }),
    enabled: aberto,
    placeholderData: (anterior) => anterior,
  });

  const { data: impressoras } = useQuery({
    queryKey: ['printers', 'select'],
    queryFn: () => printersApi.listar({ ativo: true, page: 0, size: 100 }),
    enabled: aberto,
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
    formState: { errors },
  } = useForm<DadosFormulario, unknown, DadosValidados>({
    resolver: zodResolver(esquemaOrcamento),
    defaultValues: FORMULARIO_VAZIO,
  });

  useEffect(() => {
    if (aberto) {
      const edicao = orcamento && orcamentoId !== null;
      reset(edicao ? paraFormulario(orcamento) : FORMULARIO_VAZIO);
      setClienteSelecionado(
        edicao ? ({ id: orcamento.clienteId, nome: orcamento.clienteNome } as Cliente) : null,
      );
    }
  }, [aberto, orcamento, orcamentoId, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosValidados) =>
      orcamentoId !== null
        ? quotesApi.atualizar(orcamentoId, paraPayload(dados))
        : quotesApi.criar(paraPayload(dados)),
    onSuccess: (salvo) => {
      toast.success(
        orcamentoId !== null
          ? 'Orçamento atualizado.'
          : `Orçamento ${salvo.numero} criado.`,
      );
      queryClient.invalidateQueries({ queryKey: ['quotes'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const titulo =
    orcamentoId === null
      ? 'Novo orçamento'
      : somenteLeitura
        ? `Orçamento ${orcamento?.numero ?? ''} (${
            orcamento ? ROTULOS_STATUS_ORCAMENTO[orcamento.status] : ''
          })`
        : `Editar orçamento ${orcamento?.numero ?? ''}`;

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="md" fullWidth>
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
                      sx={{ gridColumn: { sm: 'span 4' } }}
                    />
                  )}
                />
                <TextField
                  label="Validade"
                  type="date"
                  disabled={somenteLeitura}
                  slotProps={{ inputLabel: { shrink: true } }}
                  sx={{ gridColumn: { sm: 'span 2' } }}
                  {...register('dataValidade')}
                />
                <TextField
                  label="Descrição da peça/serviço"
                  disabled={somenteLeitura}
                  sx={{ gridColumn: { sm: 'span 6' } }}
                  {...register('descricao')}
                />
                <TextField
                  label="Impressora"
                  select
                  disabled={somenteLeitura}
                  slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                  helperText="Habilita energia e a config. de custos dela"
                  sx={{ gridColumn: { sm: 'span 3' } }}
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
                  disabled={somenteLeitura}
                  slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                  helperText="Habilita o custo de material"
                  sx={{ gridColumn: { sm: 'span 3' } }}
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
                  label="Tempo (min)"
                  disabled={somenteLeitura}
                  error={!!errors.tempoImpressaoMinutos}
                  helperText={errors.tempoImpressaoMinutos?.message}
                  sx={{ gridColumn: { sm: 'span 2' } }}
                  {...register('tempoImpressaoMinutos')}
                />
                <TextField
                  label="Peso (g)"
                  disabled={somenteLeitura}
                  error={!!errors.pesoEstimadoG}
                  helperText={errors.pesoEstimadoG?.message}
                  sx={{ gridColumn: { sm: 'span 2' } }}
                  {...register('pesoEstimadoG')}
                />
                <TextField
                  label="Markup (%)"
                  disabled={somenteLeitura}
                  error={!!errors.markup}
                  helperText={
                    errors.markup?.message ??
                    (orcamentoId === null ? 'Vazio usa o padrão da configuração' : undefined)
                  }
                  sx={{ gridColumn: { sm: 'span 2' } }}
                  {...register('markup')}
                />
                <TextField
                  label="Preço final (R$)"
                  disabled={somenteLeitura}
                  error={!!errors.precoFinal}
                  helperText={errors.precoFinal?.message ?? 'Vazio usa o preço sugerido'}
                  sx={{ gridColumn: { sm: 'span 3' } }}
                  {...register('precoFinal')}
                />
                <TextField
                  label="Observações"
                  disabled={somenteLeitura}
                  sx={{ gridColumn: { sm: 'span 3' } }}
                  {...register('observacoes')}
                />
              </Box>

              {orcamentoId !== null && orcamento && (
                <>
                  <Divider sx={{ my: 2 }}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Custos calculados
                    </Typography>
                  </Divider>
                  <Box
                    sx={{
                      display: 'grid',
                      gap: 3,
                      gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                    }}
                  >
                    <Box>
                      <LinhaCusto rotulo="Filamento" valor={moeda.format(orcamento.custoFilamento)} />
                      <LinhaCusto rotulo="Energia" valor={moeda.format(orcamento.custoEnergia)} />
                      <LinhaCusto
                        rotulo="Hora máquina"
                        valor={moeda.format(orcamento.custoHoraMaquina)}
                      />
                      <LinhaCusto rotulo="Desgaste" valor={moeda.format(orcamento.custoDesgaste)} />
                      <LinhaCusto
                        rotulo="Custo total"
                        valor={moeda.format(orcamento.custoTotal)}
                        destaque
                      />
                    </Box>
                    <Box>
                      <LinhaCusto rotulo="Markup" valor={`${paraCampo(orcamento.markup)}%`} />
                      <LinhaCusto
                        rotulo="Preço sugerido"
                        valor={moeda.format(orcamento.precoSugerido)}
                      />
                      <LinhaCusto
                        rotulo="Preço final negociado"
                        valor={orcamento.precoFinal != null ? moeda.format(orcamento.precoFinal) : '—'}
                      />
                      <LinhaCusto
                        rotulo="Preço ao cliente"
                        valor={moeda.format(orcamento.precoEfetivo)}
                        destaque
                      />
                    </Box>
                  </Box>
                  {orcamento.pedidoNumero && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                      Convertido no pedido <b>{orcamento.pedidoNumero}</b>.
                    </Typography>
                  )}
                </>
              )}
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
