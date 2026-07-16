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
import { clientsApi, type Cliente, type ClienteInput } from '../../api/clients';

const textoOpcional = (max: number, mensagem: string) =>
  z.union([z.literal(''), z.string().trim().max(max, mensagem)]);

const esquemaCliente = z.object({
  nome: z.string().trim().min(1, 'Informe o nome.').max(160, 'Máximo de 160 caracteres.'),
  tipoPessoa: z.enum(['FISICA', 'JURIDICA']),
  email: z.union([z.literal(''), z.email('E-mail inválido.')]),
  telefone: textoOpcional(20, 'Máximo de 20 caracteres.'),
  cpfCnpj: textoOpcional(18, 'Máximo de 18 caracteres.'),
  observacoes: z.string(),
  logradouro: textoOpcional(160, 'Máximo de 160 caracteres.'),
  numero: textoOpcional(20, 'Máximo de 20 caracteres.'),
  complemento: textoOpcional(80, 'Máximo de 80 caracteres.'),
  bairro: textoOpcional(80, 'Máximo de 80 caracteres.'),
  cidade: textoOpcional(80, 'Máximo de 80 caracteres.'),
  estado: z.union([
    z.literal(''),
    z.string().regex(/^[A-Za-z]{2}$/, 'UF com 2 letras (ex.: PR).'),
  ]),
  cep: z.union([
    z.literal(''),
    z.string().regex(/^\d{5}-?\d{3}$/, 'CEP no formato 00000-000.'),
  ]),
});

type DadosFormulario = z.infer<typeof esquemaCliente>;

const FORMULARIO_VAZIO: DadosFormulario = {
  nome: '',
  tipoPessoa: 'FISICA',
  email: '',
  telefone: '',
  cpfCnpj: '',
  observacoes: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  estado: '',
  cep: '',
};

function paraFormulario(cliente: Cliente): DadosFormulario {
  return {
    nome: cliente.nome,
    tipoPessoa: cliente.tipoPessoa,
    email: cliente.email ?? '',
    telefone: cliente.telefone ?? '',
    cpfCnpj: cliente.cpfCnpj ?? '',
    observacoes: cliente.observacoes ?? '',
    logradouro: cliente.endereco?.logradouro ?? '',
    numero: cliente.endereco?.numero ?? '',
    complemento: cliente.endereco?.complemento ?? '',
    bairro: cliente.endereco?.bairro ?? '',
    cidade: cliente.endereco?.cidade ?? '',
    estado: cliente.endereco?.estado ?? '',
    cep: cliente.endereco?.cep ?? '',
  };
}

function paraPayload(dados: DadosFormulario): ClienteInput {
  const ouNulo = (valor: string) => (valor.trim() === '' ? null : valor.trim());
  const endereco = {
    logradouro: ouNulo(dados.logradouro),
    numero: ouNulo(dados.numero),
    complemento: ouNulo(dados.complemento),
    bairro: ouNulo(dados.bairro),
    cidade: ouNulo(dados.cidade),
    estado: ouNulo(dados.estado.toUpperCase()),
    cep: ouNulo(dados.cep),
  };
  const enderecoVazio = Object.values(endereco).every((v) => v === null);
  return {
    nome: dados.nome.trim(),
    tipoPessoa: dados.tipoPessoa,
    email: ouNulo(dados.email),
    telefone: ouNulo(dados.telefone),
    cpfCnpj: ouNulo(dados.cpfCnpj),
    observacoes: ouNulo(dados.observacoes),
    endereco: enderecoVazio ? null : endereco,
  };
}

interface ClientFormDialogProps {
  aberto: boolean;
  /** Cliente em edição; null = cadastro novo. */
  cliente: Cliente | null;
  onFechar: () => void;
}

export function ClientFormDialog({ aberto, cliente, onFechar }: ClientFormDialogProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DadosFormulario>({
    resolver: zodResolver(esquemaCliente),
    defaultValues: FORMULARIO_VAZIO,
  });

  // Recarrega o formulário quando abre para outro cliente (ou para cadastro).
  useEffect(() => {
    if (aberto) {
      reset(cliente ? paraFormulario(cliente) : FORMULARIO_VAZIO);
    }
  }, [aberto, cliente, reset]);

  const salvar = useMutation({
    mutationFn: (dados: DadosFormulario) =>
      cliente
        ? clientsApi.atualizar(cliente.id, paraPayload(dados))
        : clientsApi.criar(paraPayload(dados)),
    onSuccess: () => {
      toast.success(cliente ? 'Cliente atualizado.' : 'Cliente cadastrado.');
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      onFechar();
    },
    onError: (erro) => toast.error(mensagemDeErro(erro)),
  });

  const submeter = handleSubmit((dados) => salvar.mutate(dados));

  return (
    <Dialog open={aberto} onClose={onFechar} maxWidth="md" fullWidth>
      <DialogTitle>{cliente ? `Editar cliente — ${cliente.nome}` : 'Novo cliente'}</DialogTitle>
      <form onSubmit={submeter} noValidate>
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
              label="Tipo de pessoa *"
              select
              defaultValue="FISICA"
              slotProps={{ select: { native: true } }}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('tipoPessoa')}
            >
              <option value="FISICA">Física</option>
              <option value="JURIDICA">Jurídica</option>
            </TextField>

            <TextField
              label="E-mail"
              type="email"
              error={!!errors.email}
              helperText={errors.email?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('email')}
            />
            <TextField
              label="Telefone"
              error={!!errors.telefone}
              helperText={errors.telefone?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('telefone')}
            />

            <TextField
              label="CPF/CNPJ"
              error={!!errors.cpfCnpj}
              helperText={errors.cpfCnpj?.message ?? 'Com ou sem máscara'}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('cpfCnpj')}
            />
            <TextField
              label="Observações"
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('observacoes')}
            />

            <Typography
              variant="subtitle2"
              color="text.secondary"
              sx={{ gridColumn: '1 / -1', mt: 1 }}
            >
              Endereço (opcional)
            </Typography>

            <TextField
              label="Logradouro"
              error={!!errors.logradouro}
              helperText={errors.logradouro?.message}
              sx={{ gridColumn: { sm: 'span 4' } }}
              {...register('logradouro')}
            />
            <TextField
              label="Número"
              error={!!errors.numero}
              helperText={errors.numero?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('numero')}
            />
            <TextField
              label="Complemento"
              error={!!errors.complemento}
              helperText={errors.complemento?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('complemento')}
            />
            <TextField
              label="Bairro"
              error={!!errors.bairro}
              helperText={errors.bairro?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('bairro')}
            />
            <TextField
              label="Cidade"
              error={!!errors.cidade}
              helperText={errors.cidade?.message}
              sx={{ gridColumn: { sm: 'span 3' } }}
              {...register('cidade')}
            />
            <TextField
              label="UF"
              error={!!errors.estado}
              helperText={errors.estado?.message}
              slotProps={{ htmlInput: { maxLength: 2, style: { textTransform: 'uppercase' } } }}
              sx={{ gridColumn: { sm: 'span 1' } }}
              {...register('estado')}
            />
            <TextField
              label="CEP"
              error={!!errors.cep}
              helperText={errors.cep?.message}
              sx={{ gridColumn: { sm: 'span 2' } }}
              {...register('cep')}
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
