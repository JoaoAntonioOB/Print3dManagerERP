import SearchIcon from '@mui/icons-material/Search';
import { Box, ClickAwayListener, InputBase, List, ListItemButton, ListItemText, Paper, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { clientsApi } from '../api/clients';
import { filamentsApi } from '../api/filaments';
import { ordersApi } from '../api/orders';
import { useDebounce } from '../hooks/useDebounce';

/** Busca rápida (Ctrl+K) em pedidos, clientes e filamentos — navega para a listagem do tipo escolhido. */
export function BuscaRapida() {
  const [termo, setTermo] = useState('');
  const [aberto, setAberto] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const termoEstavel = useDebounce(termo);
  const habilitado = termoEstavel.trim().length >= 2;

  useEffect(() => {
    function aoTeclar(evento: KeyboardEvent) {
      if ((evento.ctrlKey || evento.metaKey) && evento.key.toLowerCase() === 'k') {
        evento.preventDefault();
        inputRef.current?.focus();
        setAberto(true);
      }
      if (evento.key === 'Escape') {
        setAberto(false);
        inputRef.current?.blur();
      }
    }
    window.addEventListener('keydown', aoTeclar);
    return () => window.removeEventListener('keydown', aoTeclar);
  }, []);

  const { data: pedidos } = useQuery({
    queryKey: ['busca-rapida', 'pedidos', termoEstavel],
    queryFn: () => ordersApi.listar({ busca: termoEstavel, page: 0, size: 4 }),
    enabled: habilitado,
  });
  const { data: clientes } = useQuery({
    queryKey: ['busca-rapida', 'clientes', termoEstavel],
    queryFn: () => clientsApi.listar({ busca: termoEstavel, page: 0, size: 4 }),
    enabled: habilitado,
  });
  const { data: filamentos } = useQuery({
    queryKey: ['busca-rapida', 'filamentos', termoEstavel],
    queryFn: () => filamentsApi.listar({ busca: termoEstavel, page: 0, size: 4 }),
    enabled: habilitado,
  });

  const temResultados =
    (pedidos?.content.length ?? 0) + (clientes?.content.length ?? 0) + (filamentos?.content.length ?? 0) > 0;

  function ir(rota: string) {
    navigate(rota);
    setAberto(false);
    setTermo('');
    inputRef.current?.blur();
  }

  return (
    <ClickAwayListener onClickAway={() => setAberto(false)}>
      <Box sx={{ position: 'relative', width: { xs: 1, sm: 320, md: 420 } }}>
        <Paper
          variant="outlined"
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            px: 1.5,
            py: 0.5,
            bgcolor: 'background.default',
          }}
        >
          <SearchIcon fontSize="small" sx={{ color: 'text.secondary' }} />
          <InputBase
            inputRef={inputRef}
            placeholder="Buscar pedidos, clientes, filamentos..."
            value={termo}
            onChange={(evento) => {
              setTermo(evento.target.value);
              setAberto(true);
            }}
            onFocus={() => setAberto(true)}
            fullWidth
            sx={{ fontSize: '0.9rem' }}
          />
          <Typography
            variant="caption"
            sx={{
              color: 'text.secondary',
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
              px: 0.6,
              display: { xs: 'none', sm: 'block' },
              flexShrink: 0,
            }}
          >
            Ctrl K
          </Typography>
        </Paper>

        {aberto && habilitado && (
          <Paper
            sx={{
              position: 'absolute',
              top: '110%',
              left: 0,
              right: 0,
              zIndex: (t) => t.zIndex.appBar + 1,
              maxHeight: 380,
              overflowY: 'auto',
            }}
          >
            {!temResultados ? (
              <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
                Nenhum resultado para "{termoEstavel}".
              </Typography>
            ) : (
              <List dense disablePadding>
                {!!pedidos?.content.length && (
                  <>
                    <Typography
                      variant="caption"
                      sx={{ px: 2, pt: 1, display: 'block', color: 'text.secondary' }}
                    >
                      Pedidos
                    </Typography>
                    {pedidos.content.map((p) => (
                      <ListItemButton key={`pedido-${p.id}`} onClick={() => ir('/pedidos')}>
                        <ListItemText primary={`${p.numero} — ${p.clienteNome}`} />
                      </ListItemButton>
                    ))}
                  </>
                )}
                {!!clientes?.content.length && (
                  <>
                    <Typography
                      variant="caption"
                      sx={{ px: 2, pt: 1, display: 'block', color: 'text.secondary' }}
                    >
                      Clientes
                    </Typography>
                    {clientes.content.map((c) => (
                      <ListItemButton key={`cliente-${c.id}`} onClick={() => ir('/clientes')}>
                        <ListItemText primary={c.nome} />
                      </ListItemButton>
                    ))}
                  </>
                )}
                {!!filamentos?.content.length && (
                  <>
                    <Typography
                      variant="caption"
                      sx={{ px: 2, pt: 1, display: 'block', color: 'text.secondary' }}
                    >
                      Filamentos
                    </Typography>
                    {filamentos.content.map((f) => (
                      <ListItemButton key={`filamento-${f.id}`} onClick={() => ir('/filamentos')}>
                        <ListItemText primary={f.nome} />
                      </ListItemButton>
                    ))}
                  </>
                )}
              </List>
            )}
          </Paper>
        )}
      </Box>
    </ClickAwayListener>
  );
}
