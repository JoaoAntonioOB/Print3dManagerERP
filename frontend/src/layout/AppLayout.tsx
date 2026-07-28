import AddIcon from '@mui/icons-material/Add';
import AssessmentIcon from '@mui/icons-material/Assessment';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import CategoryIcon from '@mui/icons-material/Category';
import DashboardIcon from '@mui/icons-material/Dashboard';
import GroupIcon from '@mui/icons-material/Group';
import HelpOutlineIcon from '@mui/icons-material/HelpOutlineOutlined';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import KeyIcon from '@mui/icons-material/Key';
import LogoutIcon from '@mui/icons-material/Logout';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import MenuIcon from '@mui/icons-material/Menu';
import PrecisionManufacturingIcon from '@mui/icons-material/PrecisionManufacturing';
import PrintIcon from '@mui/icons-material/Print';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import RequestQuoteIcon from '@mui/icons-material/RequestQuote';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  Fab,
  IconButton,
  Link,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useState, type ReactElement } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import type { Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ChangePasswordDialog } from '../components/ChangePasswordDialog';
import { BuscaRapida } from './BuscaRapida';
import { NotificacoesMenu } from './NotificacoesMenu';

function saudacao(): string {
  const hora = new Date().getHours();
  if (hora < 12) return 'Bom dia';
  if (hora < 18) return 'Boa tarde';
  return 'Boa noite';
}

const LARGURA_MENU = 240;

const PERFIS_INTERNOS: Role[] = ['ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR'];

interface ItemMenu {
  rotulo: string;
  rota: string;
  icone: ReactElement;
  perfis: Role[];
}

interface GrupoMenu {
  /** Ausente = item de topo, fora de qualquer seção (ex.: Dashboard). */
  titulo?: string;
  itens: ItemMenu[];
}

const GRUPOS_MENU: GrupoMenu[] = [
  {
    itens: [
      { rotulo: 'Dashboard', rota: '/', icone: <DashboardIcon />, perfis: PERFIS_INTERNOS },
    ],
  },
  {
    titulo: 'Produção',
    itens: [
      { rotulo: 'Pedidos', rota: '/pedidos', icone: <ReceiptLongIcon />, perfis: PERFIS_INTERNOS },
      { rotulo: 'Impressões', rota: '/impressoes', icone: <PrintIcon />, perfis: PERFIS_INTERNOS },
      {
        rotulo: 'Orçamentos',
        rota: '/orcamentos',
        icone: <RequestQuoteIcon />,
        perfis: PERFIS_INTERNOS,
      },
    ],
  },
  {
    titulo: 'Estoque',
    itens: [
      { rotulo: 'Filamentos', rota: '/filamentos', icone: <CategoryIcon />, perfis: PERFIS_INTERNOS },
      { rotulo: 'Estoque', rota: '/estoque', icone: <Inventory2Icon />, perfis: PERFIS_INTERNOS },
    ],
  },
  {
    titulo: 'Equipamentos',
    itens: [
      {
        rotulo: 'Impressoras',
        rota: '/impressoras',
        icone: <PrecisionManufacturingIcon />,
        perfis: PERFIS_INTERNOS,
      },
    ],
  },
  {
    titulo: 'Financeiro',
    itens: [
      {
        rotulo: 'Financeiro',
        rota: '/financeiro',
        icone: <AttachMoneyIcon />,
        perfis: PERFIS_INTERNOS,
      },
    ],
  },
  {
    titulo: 'Clientes',
    itens: [{ rotulo: 'Clientes', rota: '/clientes', icone: <GroupIcon />, perfis: PERFIS_INTERNOS }],
  },
  {
    titulo: 'Relatórios',
    itens: [
      { rotulo: 'Relatórios', rota: '/relatorios', icone: <AssessmentIcon />, perfis: PERFIS_INTERNOS },
    ],
  },
  {
    titulo: 'Configurações',
    itens: [
      {
        rotulo: 'Usuários',
        rota: '/usuarios',
        icone: <ManageAccountsIcon />,
        perfis: ['ADMINISTRADOR'],
      },
    ],
  },
];

const ROTULOS_PERFIL: Record<Role, string> = {
  ADMINISTRADOR: 'Administrador',
  OPERADOR: 'Operador',
  FINANCEIRO: 'Financeiro',
  VISUALIZADOR: 'Visualizador',
  CLIENTE: 'Cliente',
};

export function AppLayout() {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();
  const [menuAberto, setMenuAberto] = useState(false);
  const [trocaSenhaAberta, setTrocaSenhaAberta] = useState(false);
  const [ajudaAberta, setAjudaAberta] = useState(false);
  const [menuUsuario, setMenuUsuario] = useState<HTMLElement | null>(null);

  const gruposVisiveis = GRUPOS_MENU.map((grupo) => ({
    ...grupo,
    itens: grupo.itens.filter((item) => usuario && item.perfis.includes(usuario.role)),
  })).filter((grupo) => grupo.itens.length > 0);

  const sair = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const conteudoMenu = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar sx={{ gap: 1 }}>
        <PrintIcon color="primary" />
        <Typography variant="subtitle1" color="primary" sx={{ fontWeight: 700 }}>
          Print3D Manager
        </Typography>
      </Toolbar>
      <Divider />
      <List sx={{ flexGrow: 1, px: 1 }}>
        {gruposVisiveis.map((grupo, indice) => (
          <Box key={grupo.titulo ?? `topo-${indice}`} sx={{ mb: 0.5 }}>
            {grupo.titulo && (
              <Typography
                variant="caption"
                sx={{
                  display: 'block',
                  px: 2,
                  pt: 2,
                  pb: 0.5,
                  color: 'text.secondary',
                  fontWeight: 700,
                  letterSpacing: '0.06em',
                  textTransform: 'uppercase',
                  fontSize: '0.7rem',
                }}
              >
                {grupo.titulo}
              </Typography>
            )}
            {grupo.itens.map((item) => (
              <ListItemButton
                key={item.rota}
                component={NavLink}
                to={item.rota}
                end={item.rota === '/'}
                onClick={() => setMenuAberto(false)}
                sx={{
                  borderRadius: 2,
                  mb: 0.5,
                  '&.active': {
                    bgcolor: (t) => alpha(t.palette.primary.main, 0.18),
                    '& .MuiListItemIcon-root, & .MuiListItemText-primary': {
                      color: 'primary.light',
                      fontWeight: 600,
                    },
                  },
                }}
              >
                <ListItemIcon>{item.icone}</ListItemIcon>
                <ListItemText primary={item.rotulo} />
              </ListItemButton>
            ))}
          </Box>
        ))}
      </List>
      <Divider />
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <Avatar sx={{ bgcolor: 'secondary.main', width: 36, height: 36 }}>
          {usuario?.nome.charAt(0).toUpperCase()}
        </Avatar>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" noWrap sx={{ fontWeight: 600 }}>
            {usuario?.nome}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {usuario ? ROTULOS_PERFIL[usuario.role] : ''}
          </Typography>
        </Box>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar sx={{ gap: 2 }}>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setMenuAberto(true)}
            sx={{ display: { md: 'none' } }}
            aria-label="Abrir menu"
          >
            <MenuIcon />
          </IconButton>

          <Box sx={{ display: { xs: 'none', md: 'block' }, minWidth: 0 }}>
            <Typography variant="subtitle1" noWrap sx={{ fontWeight: 700, lineHeight: 1.2 }}>
              {saudacao()}, {usuario?.nome.split(' ')[0]}!
            </Typography>
            <Typography variant="caption" noWrap sx={{ opacity: 0.8, display: 'block' }}>
              Aqui está o que está acontecendo na sua empresa hoje.
            </Typography>
          </Box>

          <Box sx={{ flexGrow: 1, display: 'flex', justifyContent: 'center' }}>
            <BuscaRapida />
          </Box>

          <Button
            variant="contained"
            color="secondary"
            startIcon={<AddIcon />}
            onClick={() => navigate('/pedidos?novo=1')}
            sx={{ display: { xs: 'none', sm: 'inline-flex' }, flexShrink: 0 }}
          >
            Novo pedido
          </Button>
          <IconButton
            color="inherit"
            onClick={() => navigate('/pedidos?novo=1')}
            sx={{ display: { xs: 'inline-flex', sm: 'none' } }}
            aria-label="Novo pedido"
          >
            <AddIcon />
          </IconButton>

          <NotificacoesMenu />

          <IconButton
            onClick={(e) => setMenuUsuario(e.currentTarget)}
            aria-label="Menu do usuário"
            sx={{ flexShrink: 0 }}
          >
            <Avatar sx={{ bgcolor: 'secondary.main', width: 34, height: 34 }}>
              {usuario?.nome.charAt(0).toUpperCase()}
            </Avatar>
          </IconButton>
          <Menu anchorEl={menuUsuario} open={!!menuUsuario} onClose={() => setMenuUsuario(null)}>
            <MenuItem
              onClick={() => {
                setTrocaSenhaAberta(true);
                setMenuUsuario(null);
              }}
            >
              <KeyIcon fontSize="small" sx={{ mr: 1.5 }} />
              Trocar minha senha
            </MenuItem>
            <MenuItem onClick={sair}>
              <LogoutIcon fontSize="small" sx={{ mr: 1.5 }} />
              Sair
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Fab
        color="primary"
        onClick={() => setAjudaAberta(true)}
        aria-label="Ajuda"
        sx={{ position: 'fixed', bottom: 24, right: 24, zIndex: (t) => t.zIndex.speedDial }}
      >
        <HelpOutlineIcon />
      </Fab>

      <Dialog open={ajudaAberta} onClose={() => setAjudaAberta(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Ajuda</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 1.5 }}>
            <b>Print3D Manager ERP</b> — sistema de gestão para empresas de impressão 3D.
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Atalho <b>Ctrl K</b> foca a busca rápida (pedidos, clientes, filamentos) em qualquer tela.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Documentação completa da API:{' '}
            <Link href="/api/swagger-ui.html" target="_blank" rel="noopener">
              Swagger
            </Link>
            .
          </Typography>
        </DialogContent>
      </Dialog>

      {/* Menu lateral: fixo no desktop, gaveta no mobile. */}
      <Drawer
        variant="temporary"
        open={menuAberto}
        onClose={() => setMenuAberto(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { width: LARGURA_MENU },
        }}
      >
        {conteudoMenu}
      </Drawer>
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: LARGURA_MENU,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: LARGURA_MENU, boxSizing: 'border-box' },
        }}
        open
      >
        {conteudoMenu}
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3, minWidth: 0 }}>
        <Toolbar />
        <Outlet />
      </Box>

      <ChangePasswordDialog
        aberto={trocaSenhaAberta}
        onFechar={() => setTrocaSenhaAberta(false)}
        onSenhaTrocada={() => {
          setTrocaSenhaAberta(false);
          void sair();
        }}
      />
    </Box>
  );
}
