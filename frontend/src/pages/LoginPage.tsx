import { zodResolver } from '@hookform/resolvers/zod';
import PrintIcon from '@mui/icons-material/Print';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { mensagemDeErro } from '../api/client';
import { useAuth } from '../auth/AuthContext';

const esquemaLogin = z.object({
  email: z.email('E-mail inválido.').min(1, 'Informe o e-mail.'),
  senha: z.string().min(1, 'Informe a senha.'),
});

type DadosLogin = z.infer<typeof esquemaLogin>;

export function LoginPage() {
  const { autenticado, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [erro, setErro] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DadosLogin>({ resolver: zodResolver(esquemaLogin) });

  if (autenticado) {
    return <Navigate to="/" replace />;
  }

  const destino = (location.state as { de?: string } | null)?.de ?? '/';

  const entrar = handleSubmit(async ({ email, senha }) => {
    setErro(null);
    try {
      await login(email, senha);
      navigate(destino, { replace: true });
    } catch (e) {
      setErro(mensagemDeErro(e));
    }
  });

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
      <Card sx={{ width: '100%', maxWidth: 400 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={1} sx={{ alignItems: 'center', mb: 3 }}>
            <PrintIcon color="primary" sx={{ fontSize: 44 }} />
            <Typography variant="h5" sx={{ fontWeight: 700 }}>
              Print3D Manager ERP
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Entre com sua conta para continuar
            </Typography>
          </Stack>

          <form onSubmit={entrar} noValidate>
            <Stack spacing={2}>
              {erro && <Alert severity="error">{erro}</Alert>}
              <TextField
                label="E-mail"
                type="email"
                autoComplete="email"
                autoFocus
                fullWidth
                error={!!errors.email}
                helperText={errors.email?.message}
                {...register('email')}
              />
              <TextField
                label="Senha"
                type="password"
                autoComplete="current-password"
                fullWidth
                error={!!errors.senha}
                helperText={errors.senha?.message}
                {...register('senha')}
              />
              <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                {isSubmitting ? 'Entrando…' : 'Entrar'}
              </Button>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
