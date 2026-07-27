import { useEffect, useState } from 'react';
import { AppBar, Toolbar, Typography, IconButton, Avatar, Menu, MenuItem, Box } from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import PsychologyIcon from '@mui/icons-material/Psychology';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useColorMode } from '../theme/ThemeContext';
import { getGlassSx } from '../theme/glass';

export default function Navbar({ onMenuClick }) {
  const { user, logout } = useAuth();
  const { mode, toggleColorMode } = useColorMode();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 4);
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleLogout = () => {
    setAnchorEl(null);
    logout();
    navigate('/login');
  };

  return (
    <AppBar
      position="fixed"
      color="default"
      elevation={0}
      sx={{
        zIndex: (t) => t.zIndex.drawer + 1,
        ...getGlassSx(mode),
        borderBottom: 1,
        borderColor: 'divider',
        boxShadow: scrolled ? '0 4px 20px -8px rgba(15,23,42,0.25)' : 'none',
      }}
    >
      <Toolbar>
        <IconButton
          onClick={onMenuClick}
          edge="start"
          aria-label="Open navigation menu"
          sx={{ mr: 1, display: { md: 'none' }, minWidth: 44, minHeight: 44 }}
        >
          <MenuIcon />
        </IconButton>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexGrow: 1 }}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 32,
              height: 32,
              borderRadius: '26%',
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              transition: 'transform 0.2s ease',
              '&:hover': { transform: 'scale(1.08) rotate(-4deg)' },
            }}
          >
            <PsychologyIcon fontSize="small" />
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
            IntelliMail
          </Typography>
        </Box>
        <IconButton
          onClick={toggleColorMode}
          sx={{ mr: 1, minWidth: 44, minHeight: 44 }}
          aria-label="Toggle dark mode"
        >
          {mode === 'dark' ? <Brightness7Icon /> : <Brightness4Icon />}
        </IconButton>
        <IconButton
          onClick={(e) => setAnchorEl(e.currentTarget)}
          aria-label="Account menu"
          sx={{ minWidth: 44, minHeight: 44 }}
        >
          <Avatar sx={{ width: 32, height: 32 }}>{user?.fullName?.charAt(0)?.toUpperCase() || '?'}</Avatar>
        </IconButton>
        <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
          <MenuItem
            onClick={() => {
              setAnchorEl(null);
              navigate('/profile');
            }}
          >
            Profile
          </MenuItem>
          <MenuItem
            onClick={() => {
              setAnchorEl(null);
              navigate('/settings');
            }}
          >
            Settings
          </MenuItem>
          <MenuItem onClick={handleLogout}>Logout</MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
