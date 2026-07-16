import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { z } from 'zod';
import { mensagemDeErro } from '../../api/client';
import {
  printersApi,
  type ConfiguracaoCusto,
  type ConfiguracaoCustoInput,
  type Impressora,
} from '../../api/printers';
import { useAuth } from '../../auth/AuthContext';
import { decimalOpcional, paraCampo, paraNumero } from '../../lib/form';

const esquemaConfig = z.object({
  valorKwh: decimalOpcional(),
  valorHoraMaquina: decimalOpcional(),
  custoDesgasteHora: decimalOpcional(),
  markupPadrao: decimalOpcional(),
});

type DadosConfig = z.infer<typeof esquemaConfig>;

const CONFIG_VAZIA: DadosConfig = {
  valorKwh: '',
  valorHoraMaquina: '',
  custoDesgasteHora: '',
  markupPadrao: '',
};

function paraFormulario(config: ConfiguracaoCusto | null): DadosConfig {
  if (!config) {
    return CONFIG_VAZIA;
  }
  return {
    valorKwh: paraCampo(config.valorKwh),
    valorHoraMaquina: paraCampo(config.valorHoraMaquina),
    custoDesgasteHora: paraCampo(config.custoDesgasteHora),
    markupPadrao: paraCampo(config.markupPadrao),
  };
}

function paraPayload(dados: DadosConfig): ConfiguracaoCustoInput {
  return {
    valorKwh: paraNumero(dados.valorKwh),
    valorHoraMaquina: paraNumero(dados.valorHoraMaquina),
    custoDesgasteHora: paraNumero(dados.custoDesgasteHora),
    markupPadrao: paraNumero(dados.markupPadrao),
  };
}

interface PrinterConfigDialogProps {
  aberto: boolean;
  /** Impressora alvo; null = configuração global. */
  impressora: Impressora | null;
  onFechar: () => void;
}

/**
 * Parâmetros de custo usados por orçamentos e custo real das impressões.
 * Escrita restrita a ADMINISTRADOR (regra do backend refletida na UI).
 */
export function PrinterConfigDialog({ aberto, impressora, onFechar }: PrinterConfigDialogProps) {
  const { usuario } = useAuth();
  const queryClient = useQueryClient();
  const podeEditar = usuario?.role === 'ADMINISTRADOR';

  const chaveQuery = impressora
    ? ['printers', 'config', impressora.id]
    : ['printers', 'config', 'global'];

  const { data: config, isPending } = useQuery({
    queryKey: chaveQuery,
    queryFn: () =>
      impressora
        ? printersApi.obterConfigEfetiva(impressora.id)
        : printersApi.obterConfigGlobal(),
    enabled: aberto,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosConfig>({
    resolver: zodResolver(esquemaConfig),
    defaultValues: CONFIG_VAZIA,
  });

  useEffect(() => {
    if (aberto && !isPending) {
      reset(paraFormulario(config ?? null));
    }
  }, [aberto, isPending, config, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosConfig) =>
      impressora
        ? printersApi.salvarConfigPropria(impressora.id, paraPayload(dados))
        : printersApi.salvarConfigGlobal(paraPayload(dados)),
    onSuccess: () => {
      toast.success('Configuração salva.');
      queryClient.invalidateQueries({ queryKey: ['printers', 'config'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const removerPropria = useMutation({
    mutationFn: () => printersApi.removerConfigPropria(impressora!.id),
    onSuccess: () => {
      toast.success('Configuração própria removida — a global volta a valer.');
      queryClient.invalidateQueries({ queryKey: ['printers', 'config'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const titulo = impressora
    ? `Custos — ${impressora.nome}`
    : 'Configuração global de custos';

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {titulo}
        {impressora && config?.origem && (
          <Chip
            size="small"
            color={config.origem === 'PROPRIA' ? 'secondary' : 'default'}
            label={config.origem === 'PROPRIA' ? 'Própria' : 'Herdada da global'}
          />
        )}
      </DialogTitle>
      <form onSubmit={handleSubmit((dados) => salvar.mutate(dados))} noValidate>
        <DialogContent sx={{ pt: 1 }}>
          {isPending ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={28} />
            </Box>
          ) : (
            <>
              {!config && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  {impressora
                    ? 'Nenhuma configuração cadastrada (nem própria, nem global). Os valores salvos aqui passam a valer só para esta impressora.'
                    : 'Configuração global ainda não cadastrada — ela é o padrão de custo de todas as impressoras.'}
                </Alert>
              )}
              {!podeEditar && (
                <Alert severity="warning" sx={{ mb: 2 }}>
                  Somente ADMINISTRADOR altera configurações de custo.
                </Alert>
              )}
              <Box
                sx={{
                  display: 'grid',
                  gap: 2,
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                }}
              >
                <TextField
                  label="Valor do kWh (R$)"
                  disabled={!podeEditar}
                  error={!!errors.valorKwh}
                  helperText={errors.valorKwh?.message}
                  {...register('valorKwh')}
                />
                <TextField
                  label="Valor da hora-máquina (R$)"
                  disabled={!podeEditar}
                  error={!!errors.valorHoraMaquina}
                  helperText={errors.valorHoraMaquina?.message}
                  {...register('valorHoraMaquina')}
                />
                <TextField
                  label="Desgaste por hora (R$)"
                  disabled={!podeEditar}
                  error={!!errors.custoDesgasteHora}
                  helperText={errors.custoDesgasteHora?.message}
                  {...register('custoDesgasteHora')}
                />
                <TextField
                  label="Markup padrão (%)"
                  disabled={!podeEditar}
                  error={!!errors.markupPadrao}
                  helperText={errors.markupPadrao?.message ?? 'Margem sugerida nos orçamentos'}
                  {...register('markupPadrao')}
                />
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          {impressora && podeEditar && config?.origem === 'PROPRIA' && (
            <Button
              color="error"
              onClick={() => removerPropria.mutate()}
              disabled={removerPropria.isPending}
              sx={{ mr: 'auto' }}
            >
              Remover própria
            </Button>
          )}
          <Button onClick={onFechar} color="inherit">
            Fechar
          </Button>
          {podeEditar && (
            <Button type="submit" variant="contained" disabled={salvar.isPending || isPending}>
              {salvar.isPending
                ? 'Salvando…'
                : impressora
                  ? 'Salvar própria'
                  : 'Salvar global'}
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
}
