import CancelIcon from '@mui/icons-material/Cancel';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PrintIcon from '@mui/icons-material/Print';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { useParams } from 'react-router-dom';
import { mensagemDeErro } from '../../api/client';
import {
  CORES_STATUS_ORCAMENTO,
  quotesApi,
  ROTULOS_STATUS_ORCAMENTO,
} from '../../api/quotes';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataBr = (iso: string | null) =>
  iso ? new Date(`${iso.slice(0, 10)}T00:00:00`).toLocaleDateString('pt-BR') : null;

/** Linha rótulo→valor dos dados do orçamento na visão do cliente. */
function Linha({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}>
      <Typography variant="body2" color="text.secondary">
        {rotulo}
      </Typography>
      <Typography variant="body2">{valor}</Typography>
    </Box>
  );
}

/**
 * Página pública de aprovação de orçamento — o cliente acessa pelo link com
 * shareToken, sem login (a rota /public/** do backend é liberada).
 */
export function PublicQuotePage() {
  const { shareToken } = useParams<{ shareToken: string }>();
  const queryClient = useQueryClient();

  const { data: orcamento, isPending, error } = useQuery({
    queryKey: ['public-quote', shareToken],
    queryFn: () => quotesApi.buscarPublico(shareToken!),
    enabled: !!shareToken,
    retry: false,
  });

  const responder = useMutation({
    mutationFn: (acao: 'aprovar' | 'recusar') =>
      acao === 'aprovar'
        ? quotesApi.aprovarPublico(shareToken!)
        : quotesApi.recusarPublico(shareToken!),
    onSuccess: (atualizado, acao) => {
      queryClient.setQueryData(['public-quote', shareToken], atualizado);
      toast.success(
        acao === 'aprovar'
          ? 'Orçamento aprovado. Obrigado!'
          : 'Orçamento recusado. Obrigado pelo retorno.',
      );
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const podeResponder = orcamento?.status === 'ENVIADO';

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'primary.main',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 480 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={1} sx={{ alignItems: 'center', mb: 3 }}>
            <PrintIcon color="primary" sx={{ fontSize: 44 }} />
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              Print3D Manager ERP
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Proposta de orçamento
            </Typography>
          </Stack>

          {isPending ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
              <CircularProgress size={30} />
            </Box>
          ) : error || !orcamento ? (
            <Alert severity="error">
              Orçamento não encontrado. Confira se o link recebido está completo.
            </Alert>
          ) : (
            <>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  mb: 1,
                }}
              >
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                  {orcamento.numero}
                </Typography>
                <Chip
                  size="small"
                  color={CORES_STATUS_ORCAMENTO[orcamento.status]}
                  label={ROTULOS_STATUS_ORCAMENTO[orcamento.status]}
                />
              </Box>
              <Linha rotulo="Cliente" valor={orcamento.clienteNome} />
              {orcamento.descricao && (
                <Linha rotulo="Descrição" valor={orcamento.descricao} />
              )}
              {orcamento.tempoImpressaoMinutos != null && (
                <Linha
                  rotulo="Tempo estimado"
                  valor={`${orcamento.tempoImpressaoMinutos} min`}
                />
              )}
              {orcamento.pesoEstimadoG != null && (
                <Linha
                  rotulo="Peso estimado"
                  valor={`${String(orcamento.pesoEstimadoG).replace('.', ',')} g`}
                />
              )}
              {dataBr(orcamento.dataValidade) && (
                <Linha rotulo="Válido até" valor={dataBr(orcamento.dataValidade)!} />
              )}

              <Divider sx={{ my: 2 }} />
              <Box sx={{ textAlign: 'center', mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Valor proposto
                </Typography>
                <Typography variant="h4" color="primary" sx={{ fontWeight: 700 }}>
                  {moeda.format(orcamento.preco)}
                </Typography>
              </Box>

              {podeResponder ? (
                <Stack direction="row" spacing={2} sx={{ justifyContent: 'center' }}>
                  <Button
                    variant="contained"
                    color="success"
                    startIcon={<CheckCircleIcon />}
                    disabled={responder.isPending}
                    onClick={() => responder.mutate('aprovar')}
                  >
                    Aprovar
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    startIcon={<CancelIcon />}
                    disabled={responder.isPending}
                    onClick={() => responder.mutate('recusar')}
                  >
                    Recusar
                  </Button>
                </Stack>
              ) : (
                <Alert
                  severity={
                    orcamento.status === 'APROVADO' || orcamento.status === 'CONVERTIDO'
                      ? 'success'
                      : orcamento.status === 'REJEITADO'
                        ? 'warning'
                        : 'info'
                  }
                >
                  {orcamento.status === 'APROVADO' || orcamento.status === 'CONVERTIDO'
                    ? 'Este orçamento já foi aprovado. Obrigado!'
                    : orcamento.status === 'REJEITADO'
                      ? 'Este orçamento foi recusado.'
                      : orcamento.status === 'EXPIRADO'
                        ? 'Este orçamento expirou. Solicite uma nova proposta.'
                        : 'Este orçamento não está disponível para resposta.'}
                </Alert>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
