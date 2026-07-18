import AssessmentIcon from '@mui/icons-material/Assessment';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import CategoryIcon from '@mui/icons-material/Category';
import DashboardIcon from '@mui/icons-material/Dashboard';
import GroupIcon from '@mui/icons-material/Group';
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
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import { useState, type ReactElement } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import type { Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ChangePasswordDialog } from '../components/ChangePasswordDialog';

const LARGURA_MENU = 240;

const PERFIS_INTERNOS: Role[] = ['ADMINISTRADOR', 'OPERADOR', 'FINANCEIRO', 'VISUALIZADOR'];

interface ItemMenu {
  rotulo: string;
  rota: string;
  icone: ReactElement;
  perfis: Role[];
}

const ITENS_MENU: ItemMenu[] = [
  { rotulo: 'Dashboard', rota: '/', icone: <DashboardIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Pedidos', rota: '/pedidos', icone: <ReceiptLongIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Orçamentos', rota: '/orcamentos', icone: <RequestQuoteIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Clientes', rota: '/clientes', icone: <GroupIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Filamentos', rota: '/filamentos', icone: <CategoryIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Estoque', rota: '/estoque', icone: <Inventory2Icon />, perfis: PERFIS_INTERNOS },
  {
    rotulo: 'Impressoras',
    rota: '/impressoras',
    icone: <PrecisionManufacturingIcon />,
    perfis: PERFIS_INTERNOS,
  },
  { rotulo: 'Impressões', rota: '/impressoes', icone: <PrintIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Financeiro', rota: '/financeiro', icone: <AttachMoneyIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Relatórios', rota: '/relatorios', icone: <AssessmentIcon />, perfis: PERFIS_INTERNOS },
  { rotulo: 'Usuários', rota: '/usuarios', icone: <ManageAccountsIcon />, perfis: ['ADMINISTRADOR'] },
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

  const itensVisiveis = ITENS_MENU.filter(
    (item) => usuario && item.perfis.includes(usuario.role),
  );

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
      <List sx={{ flexGrow: 1 }}>
        {itensVisiveis.map((item) => (
          <ListItemButton
            key={item.rota}
            component={NavLink}
            to={item.rota}
            end={item.rota === '/'}
            onClick={() => setMenuAberto(false)}
            sx={{
              '&.active': {
                bgcolor: 'action.selected',
                borderRight: 3,
                borderColor: 'primary.main',
                '& .MuiListItemIcon-root, & .MuiListItemText-primary': {
                  color: 'primary.main',
                  fontWeight: 600,
                },
              },
            }}
          >
            <ListItemIcon>{item.icone}</ListItemIcon>
            <ListItemText primary={item.rotulo} />
          </ListItemButton>
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
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setMenuAberto(true)}
            sx={{ mr: 1, display: { md: 'none' } }}
            aria-label="Abrir menu"
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1 }} noWrap>
            Print3D Manager ERP
          </Typography>
          <Tooltip title="Trocar minha senha">
            <IconButton
              color="inherit"
              onClick={() => setTrocaSenhaAberta(true)}
              aria-label="Trocar minha senha"
            >
              <KeyIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Sair">
            <IconButton color="inherit" onClick={sair} aria-label="Sair">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

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
