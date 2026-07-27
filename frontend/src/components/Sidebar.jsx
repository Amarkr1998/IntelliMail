import { Drawer, List, ListItemButton, ListItemIcon, ListItemText, ListSubheader, Toolbar, useMediaQuery } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import DashboardIcon from '@mui/icons-material/Dashboard';
import EditNoteIcon from '@mui/icons-material/EditNote';
import MicIcon from '@mui/icons-material/Mic';
import HistoryIcon from '@mui/icons-material/History';
import ArticleIcon from '@mui/icons-material/Article';
import BarChartIcon from '@mui/icons-material/BarChart';
import SettingsIcon from '@mui/icons-material/Settings';
import GroupIcon from '@mui/icons-material/Group';
import CreditCardIcon from '@mui/icons-material/CreditCard';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const drawerWidth = 220;

const mainNavItems = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'Compose Assistant', path: '/compose', icon: <EditNoteIcon /> },
  { label: 'Voice AI', path: '/voice-ai', icon: <MicIcon /> },
  { label: 'History', path: '/history', icon: <HistoryIcon /> },
  { label: 'Templates', path: '/templates', icon: <ArticleIcon /> },
  { label: 'Analytics', path: '/analytics', icon: <BarChartIcon /> },
  { label: 'Settings', path: '/settings', icon: <SettingsIcon /> },
];

const organizationNavItems = [
  { label: 'Team', path: '/organization', icon: <GroupIcon /> },
  { label: 'Billing', path: '/billing', icon: <CreditCardIcon /> },
];

function NavItems({ items, onNavigate }) {
  const location = useLocation();

  return (
    <>
      {items.map((item) => {
        const selected = location.pathname === item.path;
        return (
          <ListItemButton
            key={item.path}
            component={NavLink}
            to={item.path}
            selected={selected}
            onClick={onNavigate}
            sx={{
              borderRadius: 2.5,
              mb: 0.5,
              minHeight: 44,
              transition: 'background-color 0.18s ease, transform 0.18s ease, padding-left 0.18s ease',
              '&:hover': { pl: 2 },
              '& .MuiListItemIcon-root': { transition: 'transform 0.18s ease, color 0.18s ease' },
              '&:hover .MuiListItemIcon-root': { transform: 'scale(1.12)' },
              '&.Mui-selected': {
                bgcolor: (theme) => (theme.palette.mode === 'dark' ? 'rgba(129,140,248,0.16)' : 'rgba(79,70,229,0.1)'),
                '&:hover': { bgcolor: (theme) => (theme.palette.mode === 'dark' ? 'rgba(129,140,248,0.22)' : 'rgba(79,70,229,0.16)') },
              },
            }}
          >
            <ListItemIcon sx={{ color: selected ? 'primary.main' : 'inherit', minWidth: 40 }}>{item.icon}</ListItemIcon>
            <ListItemText
              primary={item.label}
              slotProps={{ primary: { fontWeight: selected ? 700 : 500 } }}
            />
          </ListItemButton>
        );
      })}
    </>
  );
}

function NavList({ onNavigate }) {
  const { user } = useAuth();

  return (
    <List sx={{ px: 1.25 }}>
      <NavItems items={mainNavItems} onNavigate={onNavigate} />
      {user?.organizationId && (
        <>
          <ListSubheader sx={{ bgcolor: 'transparent', lineHeight: '32px' }}>Organization</ListSubheader>
          <NavItems items={organizationNavItems} onNavigate={onNavigate} />
        </>
      )}
    </List>
  );
}

export default function Sidebar({ mobileOpen = false, onMobileClose }) {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));

  if (isDesktop) {
    return (
      <Drawer
        variant="permanent"
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box', borderRight: 1, borderColor: 'divider' },
        }}
      >
        <Toolbar />
        <NavList />
      </Drawer>
    );
  }

  return (
    <Drawer
      variant="temporary"
      open={mobileOpen}
      onClose={onMobileClose}
      ModalProps={{ keepMounted: true }}
      sx={{ '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' } }}
    >
      <Toolbar />
      <NavList onNavigate={onMobileClose} />
    </Drawer>
  );
}
