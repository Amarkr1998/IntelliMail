import { Box, Paper, Stack, Switch, FormControlLabel, Typography, Divider, Button } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import GroupIcon from '@mui/icons-material/Group';
import { useColorMode } from '../theme/ThemeContext';
import { useAuth } from '../context/AuthContext';
import PageHeader from '../components/common/PageHeader';

export default function SettingsPage() {
  const { mode, toggleColorMode } = useColorMode();
  const { user } = useAuth();

  return (
    <Box>
      <PageHeader title="Settings" subtitle="Manage how IntelliMail looks and feels." />
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480, mb: 3 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 40,
              height: 40,
              borderRadius: '30%',
              bgcolor: 'action.hover',
              color: 'primary.main',
            }}
          >
            <DarkModeIcon />
          </Box>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="subtitle2">Appearance</Typography>
            <Typography variant="caption" color="text.secondary">
              Switch between light and dark mode
            </Typography>
          </Box>
        </Stack>
        <Divider sx={{ my: 2 }} />
        <FormControlLabel
          control={<Switch checked={mode === 'dark'} onChange={toggleColorMode} />}
          label="Dark mode"
        />
      </Paper>

      <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 40,
              height: 40,
              borderRadius: '30%',
              bgcolor: 'action.hover',
              color: 'primary.main',
            }}
          >
            <GroupIcon />
          </Box>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="subtitle2">Organization</Typography>
            <Typography variant="caption" color="text.secondary">
              {user?.organizationId
                ? `You're a member of ${user.organizationName}`
                : 'Optional - invite teammates and manage billing together'}
            </Typography>
          </Box>
          <Button
            component={RouterLink}
            to={user?.organizationId ? '/organization' : '/create-organization'}
            variant="outlined"
            size="small"
          >
            {user?.organizationId ? 'Manage' : 'Create'}
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
}
